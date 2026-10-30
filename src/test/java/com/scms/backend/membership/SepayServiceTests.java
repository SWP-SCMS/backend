package com.scms.backend.membership;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class SepayServiceTests {
	private static final String BANK_DESTINATION_SHA256 =
		"7128318f4501a30f6c5648627306786fb6e7a26a15da6850a959c5b35a0a6859";

	@Mock JdbcTemplate db;
	@Mock AccountRepository accounts;
	@Mock PaymentFulfillmentService fulfillment;

	@Test
	void receptionistPaymentRequiresAnActiveReceptionist() {
		UUID actor = UUID.randomUUID();
		when(accounts.existsByIdAndRoleAndStatus(actor, AccountRole.RECEPTIONIST, AccountStatus.ACTIVE))
			.thenReturn(false);
		SepayService service = service("MB", "123456789", "SCMS GYM");

		assertThatThrownBy(() -> service.createForReceptionist(actor, "MB-000001", UUID.randomUUID()))
			.isInstanceOf(com.scms.backend.auth.InvalidAuthenticatedAccountException.class);
	}

	@Test
	void blankAccountNameRejectsNewPaymentBeforeWrites() {
		UUID actor = UUID.randomUUID();
		UUID orderId = UUID.randomUUID();
		when(accounts.existsByIdAndRoleAndStatus(actor, AccountRole.MEMBER, AccountStatus.ACTIVE)).thenReturn(true);
		when(db.queryForMap("select * from membership_orders where id=? and member_account_id=? for update",
			orderId, actor)).thenReturn(Map.of("status", "PENDING_PAYMENT"));
		when(db.queryForList(
			"select * from payments where order_id=? and status='PENDING' order by created_at limit 1", orderId))
			.thenReturn(List.of());
		SepayService service = service("MB", "123456789", "  ");

		assertThatThrownBy(() -> service.create(actor, orderId))
			.isInstanceOf(PaymentException.class)
			.hasMessage("SePay bank configuration is missing");
		verify(db, never()).update(any(String.class), any(Object[].class));
	}

	@Test
	void matchingFingerprintAllowsCanonicalDestinationCreation() {
		UUID actor = UUID.randomUUID();
		UUID orderId = UUID.randomUUID();
		UUID paymentId = UUID.randomUUID();
		when(accounts.existsByIdAndRoleAndStatus(actor, AccountRole.MEMBER, AccountStatus.ACTIVE)).thenReturn(true);
		when(db.queryForMap("select * from membership_orders where id=? and member_account_id=? for update",
			orderId, actor)).thenReturn(Map.of(
			"status", "PENDING_PAYMENT", "order_number", "ORD-123", "price_amount_snapshot", BigDecimal.valueOf(12000)));
		when(db.queryForList(
			"select * from payments where order_id=? and status='PENDING' order by created_at limit 1", orderId))
			.thenReturn(List.of());
		when(db.queryForMap(eq("select * from payments where id=?"), any(UUID.class))).thenReturn(Map.of(
			"id", paymentId, "order_id", orderId, "amount", BigDecimal.valueOf(12000), "currency_code", "VND",
			"bank_transfer_content", "SCMS-ORD-123", "bank_code_snapshot", "mb",
			"bank_account_number_snapshot", "123456789", "bank_account_name_snapshot", "scms gym",
			"status", "PENDING"));

		assertThatCode(() -> service(" mb ", " 123456789 ", " scms gym ", BANK_DESTINATION_SHA256)
			.create(actor, orderId)).doesNotThrowAnyException();

		verify(db, times(2)).update(any(String.class), any(Object[].class));
	}

	@ParameterizedTest
	@ValueSource(strings = { "", "not-hex", "0000000000000000000000000000000000000000000000000000000000000000" })
	void missingOrMismatchedFingerprintRejectsCreationBeforeWrites(String fingerprint) {
		UUID actor = UUID.randomUUID();
		UUID orderId = UUID.randomUUID();
		when(accounts.existsByIdAndRoleAndStatus(actor, AccountRole.MEMBER, AccountStatus.ACTIVE)).thenReturn(true);
		when(db.queryForMap("select * from membership_orders where id=? and member_account_id=? for update",
			orderId, actor)).thenReturn(Map.of("status", "PENDING_PAYMENT"));
		when(db.queryForList(
			"select * from payments where order_id=? and status='PENDING' order by created_at limit 1", orderId))
			.thenReturn(List.of());

		assertThatThrownBy(() -> service("MB", "123456789", "SCMS GYM", fingerprint).create(actor, orderId))
			.isInstanceOfSatisfying(PaymentException.class, exception -> {
				assertThat(exception.status()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
				assertThat(exception.code()).isEqualTo("PAYMENT_DESTINATION_CONFIGURATION_INVALID");
			});
		verify(db, never()).update(any(String.class), any(Object[].class));
	}

	@Test
	void retryUsesPersistedDestinationAfterConfigurationRotation() {
		UUID actor = UUID.randomUUID();
		UUID orderId = UUID.randomUUID();
		UUID paymentId = UUID.randomUUID();
		Instant expiresAt = Instant.parse("2026-10-09T00:00:00Z");
		Map<String, Object> order = Map.of(
			"status", "PENDING_PAYMENT", "order_number", "ORD-123", "expires_at", Timestamp.from(expiresAt));
		Map<String, Object> payment = Map.of(
			"id", paymentId, "order_id", orderId, "amount", BigDecimal.valueOf(12000), "currency_code", "VND",
			"bank_transfer_content", "SCMS-ORD-123", "bank_code_snapshot", "VCB",
			"bank_account_number_snapshot", "old-account", "bank_account_name_snapshot", "OLD GYM",
			"status", "PENDING");
		when(accounts.existsByIdAndRoleAndStatus(actor, AccountRole.MEMBER, AccountStatus.ACTIVE)).thenReturn(true);
		when(db.queryForMap("select * from membership_orders where id=? and member_account_id=? for update",
			orderId, actor)).thenReturn(order);
		when(db.queryForList(
			"select * from payments where order_id=? and status='PENDING' order by created_at limit 1", orderId))
			.thenReturn(List.of(payment));
		SepayService rotatedService = service("MB", "new-account", "NEW GYM");

		SepayPaymentResponse response = rotatedService.create(actor, orderId);

		assertThat(response.bankCode()).isEqualTo("VCB");
		assertThat(response.bankAccountNumber()).isEqualTo("old-account");
		assertThat(response.bankAccountName()).isEqualTo("OLD GYM");
		assertThat(response.qrUrl()).contains("bank=VCB", "acc=old-account", "accountName=OLD+GYM");
	}

	@Test
	void retryUsesCompleteSnapshotWhenCurrentConfigurationIsBlank() {
		UUID actor = UUID.randomUUID();
		UUID orderId = UUID.randomUUID();
		Instant expiresAt = Instant.parse("2026-10-09T00:00:00Z");
		Map<String, Object> order = Map.of(
			"status", "PENDING_PAYMENT", "order_number", "ORD-123", "expires_at", Timestamp.from(expiresAt));
		Map<String, Object> payment = Map.of(
			"id", UUID.randomUUID(), "order_id", orderId, "amount", BigDecimal.valueOf(12000),
			"currency_code", "VND", "bank_transfer_content", "SCMS-ORD-123", "bank_code_snapshot", "VCB",
			"bank_account_number_snapshot", "old-account", "bank_account_name_snapshot", "OLD GYM",
			"status", "PENDING");
		when(accounts.existsByIdAndRoleAndStatus(actor, AccountRole.MEMBER, AccountStatus.ACTIVE)).thenReturn(true);
		when(db.queryForMap("select * from membership_orders where id=? and member_account_id=? for update",
			orderId, actor)).thenReturn(order);
		when(db.queryForList(
			"select * from payments where order_id=? and status='PENDING' order by created_at limit 1", orderId))
			.thenReturn(List.of(payment));

		AtomicReference<SepayPaymentResponse> response = new AtomicReference<>();
		assertThatCode(() -> response.set(service("", "", "").create(actor, orderId)))
			.doesNotThrowAnyException();

		assertThat(response.get().bankCode()).isEqualTo("VCB");
		assertThat(response.get().bankAccountNumber()).isEqualTo("old-account");
		assertThat(response.get().bankAccountName()).isEqualTo("OLD GYM");
	}

	@Test
	void legacyPaymentRetryDoesNotBackfillSnapshotFromRuntimeConfiguration() {
		UUID actor = UUID.randomUUID();
		UUID orderId = UUID.randomUUID();
		Instant expiresAt = Instant.parse("2026-10-09T00:00:00Z");
		Map<String, Object> order = Map.of(
			"status", "PENDING_PAYMENT", "order_number", "ORD-123", "expires_at", Timestamp.from(expiresAt));
		Map<String, Object> payment = new HashMap<>();
		payment.put("id", UUID.randomUUID());
		payment.put("order_id", orderId);
		payment.put("amount", BigDecimal.valueOf(12000));
		payment.put("currency_code", "VND");
		payment.put("bank_transfer_content", "SCMS-ORD-123");
		payment.put("bank_code_snapshot", null);
		payment.put("bank_account_number_snapshot", null);
		payment.put("bank_account_name_snapshot", null);
		payment.put("status", "PENDING");
		when(accounts.existsByIdAndRoleAndStatus(actor, AccountRole.MEMBER, AccountStatus.ACTIVE)).thenReturn(true);
		when(db.queryForMap("select * from membership_orders where id=? and member_account_id=? for update",
			orderId, actor)).thenReturn(order);
		when(db.queryForList(
			"select * from payments where order_id=? and status='PENDING' order by created_at limit 1", orderId))
			.thenReturn(List.of(payment));

		assertThatThrownBy(() -> service("MB", "123456789", "SCMS GYM").create(actor, orderId))
			.isInstanceOfSatisfying(PaymentException.class, exception -> {
				assertThat(exception.status()).isEqualTo(HttpStatus.CONFLICT);
				assertThat(exception.code()).isEqualTo("PAYMENT_DESTINATION_SNAPSHOT_MISSING");
			});
		verify(db, never()).update(any(String.class), any(Object[].class));
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"bank_code_snapshot", "bank_account_number_snapshot", "bank_account_name_snapshot"
	})
	void legacyPaymentRetryRejectsAnyBlankDestinationSnapshot(String blankSnapshot) {
		UUID actor = UUID.randomUUID();
		UUID orderId = UUID.randomUUID();
		Map<String, Object> payment = new HashMap<>();
		payment.put("id", UUID.randomUUID());
		payment.put("order_id", orderId);
		payment.put("bank_code_snapshot", "MB");
		payment.put("bank_account_number_snapshot", "123456789");
		payment.put("bank_account_name_snapshot", "SCMS GYM");
		payment.put(blankSnapshot, " ");
		when(accounts.existsByIdAndRoleAndStatus(actor, AccountRole.MEMBER, AccountStatus.ACTIVE)).thenReturn(true);
		when(db.queryForMap("select * from membership_orders where id=? and member_account_id=? for update",
			orderId, actor)).thenReturn(Map.of(
				"status", "PENDING_PAYMENT", "expires_at", Timestamp.from(Instant.parse("2026-10-09T00:00:00Z"))));
		when(db.queryForList(
			"select * from payments where order_id=? and status='PENDING' order by created_at limit 1", orderId))
			.thenReturn(List.of(payment));

		assertThatThrownBy(() -> service("MB", "123456789", "SCMS GYM").create(actor, orderId))
			.isInstanceOfSatisfying(PaymentException.class, exception ->
				assertThat(exception.code()).isEqualTo("PAYMENT_DESTINATION_SNAPSHOT_MISSING"));
		verify(db, never()).update(any(String.class), any(Object[].class));
	}

	@Test
	void courseReferenceRoutesOnlyToCoursePayment() {
		UUID coursePaymentId = UUID.randomUUID();
		UUID courseOrderId = UUID.randomUUID();
		String reference = "COR-0123456789ABCDEF0123456789ABCDEF";
		SepayWebhookRequest request = new SepayWebhookRequest(9000L, reference, "PAY " + reference,
			BigInteger.valueOf(12000), "in", null, null, "123456789");
		when(db.queryForObject("select id from payments where provider='SEPAY' and provider_reference=?",
			UUID.class, reference)).thenThrow(new org.springframework.dao.EmptyResultDataAccessException(1));
		when(db.queryForObject(
			"select id from payments where provider='SEPAY' and replace(provider_reference, '-', '')=?",
			UUID.class, "COR0123456789ABCDEF0123456789ABCDEF"))
			.thenThrow(new org.springframework.dao.EmptyResultDataAccessException(1));
		when(db.queryForObject("select id from course_payments where payment_reference=?", UUID.class, reference))
			.thenReturn(coursePaymentId);
		when(fulfillment.processSepayWebhook(eq(null), eq(coursePaymentId), eq(request),
			eq("COR0123456789ABCDEF0123456789ABCDEF"), anyString(),
			eq(Instant.parse("2026-10-08T00:00:00Z"))))
			.thenReturn(new SepayWebhookResponse(true, false, SepayWebhookOutcome.COURSE_PAYMENT_RECORDED,
				coursePaymentId, courseOrderId, "PENDING"));

		SepayWebhookResponse response = service("MB", "123456789", "SCMS GYM")
			.webhook("hook-secret", request);

		assertThat(response.outcome()).isEqualTo(SepayWebhookOutcome.COURSE_PAYMENT_RECORDED);
		assertThat(response.processed()).isFalse();
		assertThat(response.paymentId()).isEqualTo(coursePaymentId);
		verify(fulfillment).processSepayWebhook(eq(null), eq(coursePaymentId), eq(request),
			eq("COR0123456789ABCDEF0123456789ABCDEF"), anyString(),
			eq(Instant.parse("2026-10-08T00:00:00Z")));
	}

	@Test
	void webhookAtExactExpirationIsAcknowledgedButNotFulfilled() {
		UUID paymentId = UUID.randomUUID();
		when(db.queryForObject("select id from payments where provider='SEPAY' and provider_reference=?",
			UUID.class, "SCMS-ORD-123")).thenReturn(paymentId);
		SepayWebhookRequest request = new SepayWebhookRequest(9001L, "SCMS-ORD-123", "PAY SCMS-ORD-123",
			BigInteger.valueOf(12000), "in", null, null, "123456789");
		when(fulfillment.processSepayWebhook(eq(paymentId), eq(null), eq(request), eq("SCMSORD123"), anyString(),
			any(Instant.class)))
			.thenReturn(new SepayWebhookResponse(true, false, SepayWebhookOutcome.PAYMENT_WINDOW_EXPIRED,
				paymentId, UUID.randomUUID(), "PENDING"));

		SepayWebhookResponse response = service("MB", "123456789", "SCMS GYM")
			.webhook("hook-secret", request);

		assertThat(response).isNotNull();
		assertThat(response.success()).isTrue();
		assertThat(response.processed()).isFalse();
		assertThat(response.outcome()).isEqualTo(SepayWebhookOutcome.PAYMENT_WINDOW_EXPIRED);
		verify(fulfillment, never()).fulfill(any(), any(), any(), any(), any());
	}

	@Test
	void webhookAfterExpirationIsAcknowledgedButNotFulfilled() {
		UUID paymentId = UUID.randomUUID();
		when(db.queryForObject("select id from payments where provider='SEPAY' and provider_reference=?",
			UUID.class, "SCMS-ORD-123")).thenReturn(paymentId);
		SepayWebhookRequest request = new SepayWebhookRequest(9001L, "SCMS-ORD-123", "PAY SCMS-ORD-123",
			BigInteger.valueOf(12000), "in", null, null, "123456789");
		when(fulfillment.processSepayWebhook(eq(paymentId), eq(null), eq(request), eq("SCMSORD123"), anyString(),
			any(Instant.class)))
			.thenReturn(new SepayWebhookResponse(true, false, SepayWebhookOutcome.PAYMENT_WINDOW_EXPIRED,
				paymentId, UUID.randomUUID(), "PENDING"));

		SepayWebhookResponse response = service("MB", "123456789", "SCMS GYM")
			.webhook("hook-secret", request);

		assertThat(response).isNotNull();
		assertThat(response.success()).isTrue();
		assertThat(response.processed()).isFalse();
		assertThat(response.outcome()).isEqualTo(SepayWebhookOutcome.PAYMENT_WINDOW_EXPIRED);
		verify(fulfillment, never()).fulfill(any(), any(), any(), any(), any());
	}

	@Test
	void webhookRejectsAutomaticFulfillmentWhenExpirationIsMissing() {
		UUID paymentId = UUID.randomUUID();
		when(db.queryForObject("select id from payments where provider='SEPAY' and provider_reference=?",
			UUID.class, "SCMS-ORD-123")).thenReturn(paymentId);
		SepayWebhookRequest request = new SepayWebhookRequest(9001L, "SCMS-ORD-123", "PAY SCMS-ORD-123",
			BigInteger.valueOf(12000), "in", null, null, "123456789");
		when(fulfillment.processSepayWebhook(eq(paymentId), eq(null), eq(request), eq("SCMSORD123"), anyString(),
			any(Instant.class)))
			.thenReturn(new SepayWebhookResponse(true, false, SepayWebhookOutcome.PAYMENT_WINDOW_EXPIRED,
				paymentId, UUID.randomUUID(), "PENDING"));

		SepayWebhookResponse response = service("MB", "123456789", "SCMS GYM")
			.webhook("hook-secret", request);

		assertThat(response).isNotNull();
		assertThat(response.success()).isTrue();
		assertThat(response.processed()).isFalse();
		assertThat(response.outcome()).isEqualTo(SepayWebhookOutcome.PAYMENT_WINDOW_EXPIRED);
		verify(fulfillment, never()).fulfill(any(), any(), any(), any(), any());
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"bank_code_snapshot", "bank_account_number_snapshot", "bank_account_name_snapshot"
	})
	void webhookRejectsAutomaticFulfillmentWhenAnyDestinationSnapshotIsMissing(String missingSnapshot) {
		UUID paymentId = UUID.randomUUID();
		when(db.queryForObject("select id from payments where provider='SEPAY' and provider_reference=?",
			UUID.class, "SCMS-ORD-123")).thenReturn(paymentId);
		SepayWebhookRequest request = new SepayWebhookRequest(9001L, "SCMS-ORD-123", "PAY SCMS-ORD-123",
			BigInteger.valueOf(12000), "in", null, null, "123456789");
		when(fulfillment.processSepayWebhook(eq(paymentId), eq(null), eq(request), eq("SCMSORD123"), anyString(),
			any(Instant.class)))
			.thenReturn(new SepayWebhookResponse(true, false, SepayWebhookOutcome.DESTINATION_SNAPSHOT_MISSING,
				paymentId, UUID.randomUUID(), "PENDING"));

		SepayWebhookResponse response = service("MB", "123456789", "SCMS GYM")
			.webhook("hook-secret", request);

		assertThat(response).isNotNull();
		assertThat(response.success()).isTrue();
		assertThat(response.processed()).isFalse();
		assertThat(response.outcome()).isEqualTo(SepayWebhookOutcome.DESTINATION_SNAPSHOT_MISSING);
		verify(fulfillment, never()).fulfill(any(), any(), any(), any(), any());
	}

	@Test
	void webhookUsesPersistedAccountAfterConfigurationRotation() {
		UUID paymentId = UUID.randomUUID();
		when(db.queryForObject("select id from payments where provider='SEPAY' and provider_reference=?",
			UUID.class, "SCMS-ORD-123")).thenReturn(paymentId);
		SepayWebhookRequest request = new SepayWebhookRequest(9001L, "SCMS-ORD-123", "PAY SCMS-ORD-123",
			BigInteger.valueOf(12000), "in", null, null, "old-account");
		when(fulfillment.processSepayWebhook(eq(paymentId), eq(null), eq(request), eq("SCMSORD123"), anyString(),
			any(Instant.class)))
			.thenReturn(new SepayWebhookResponse(true, true, SepayWebhookOutcome.FULFILLED,
				paymentId, UUID.randomUUID(), "PAID"));

		assertThatCode(() -> service("MB", "new-account", "NEW GYM").webhook("hook-secret", request))
			.doesNotThrowAnyException();

		verify(fulfillment).processSepayWebhook(eq(paymentId), eq(null), eq(request), eq("SCMSORD123"), anyString(),
			any(Instant.class));
	}

	@Test
	void newPaymentPersistsCompleteDestinationSnapshot() {
		UUID actor = UUID.randomUUID();
		UUID orderId = UUID.randomUUID();
		UUID paymentId = UUID.randomUUID();
		Map<String, Object> order = Map.of(
			"status", "PENDING_PAYMENT", "order_number", "ORD-123",
			"price_amount_snapshot", BigDecimal.valueOf(12000));
		Map<String, Object> payment = Map.of(
			"id", paymentId, "order_id", orderId, "amount", BigDecimal.valueOf(12000), "currency_code", "VND",
			"bank_transfer_content", "SCMS-ORD-123", "bank_code_snapshot", "MB",
			"bank_account_number_snapshot", "123456789", "bank_account_name_snapshot", "SCMS GYM",
			"status", "PENDING");
		when(accounts.existsByIdAndRoleAndStatus(actor, AccountRole.MEMBER, AccountStatus.ACTIVE)).thenReturn(true);
		when(db.queryForMap("select * from membership_orders where id=? and member_account_id=? for update",
			orderId, actor)).thenReturn(order);
		when(db.queryForList(
			"select * from payments where order_id=? and status='PENDING' order by created_at limit 1", orderId))
			.thenReturn(List.of());
		when(db.queryForMap(eq("select * from payments where id=?"), any(UUID.class))).thenReturn(payment);

		service("MB", "123456789", "SCMS GYM").create(actor, orderId);

		ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<Object[]> args = ArgumentCaptor.forClass(Object[].class);
		verify(db, times(2)).update(sql.capture(), args.capture());
		int insert = sql.getAllValues().indexOf(sql.getAllValues().stream()
			.filter(value -> value.contains("insert into payments")).findFirst().orElseThrow());
		assertThat(sql.getAllValues().get(insert)).contains(
			"bank_code_snapshot", "bank_account_number_snapshot", "bank_account_name_snapshot");
		assertThat(args.getAllValues().get(insert)).containsSequence(
			"SCMS-ORD-123", "MB", "123456789", "SCMS GYM", "SCMS-ORD-123");
	}

	@Test
	void responseAndQrPreserveExactPersistedTransferContent() {
		UUID actor = UUID.randomUUID();
		UUID orderId = UUID.randomUUID();
		UUID paymentId = UUID.randomUUID();
		Instant expiresAt = Instant.parse("2026-10-09T00:00:00Z");
		String persistedContent = "  SCMS-EXACT CONTENT  ";
		Map<String, Object> order = Map.of(
			"status", "PENDING_PAYMENT", "order_number", "ORD-123", "expires_at", Timestamp.from(expiresAt));
		Map<String, Object> payment = Map.of(
			"id", paymentId, "order_id", orderId, "amount", BigDecimal.valueOf(12000), "currency_code", "VND",
			"bank_transfer_content", persistedContent, "bank_code_snapshot", "MB",
			"bank_account_number_snapshot", "123456789", "bank_account_name_snapshot", "SCMS GYM",
			"status", "PENDING");
		when(accounts.existsByIdAndRoleAndStatus(actor, AccountRole.MEMBER, AccountStatus.ACTIVE)).thenReturn(true);
		when(db.queryForMap("select * from membership_orders where id=? and member_account_id=? for update",
			orderId, actor)).thenReturn(order);
		when(db.queryForList(
			"select * from payments where order_id=? and status='PENDING' order by created_at limit 1", orderId))
			.thenReturn(List.of(payment));
		SepayService service = service("MB", "123456789", "SCMS GYM");

		SepayPaymentResponse response = service.create(actor, orderId);
		String encodedDescription = Arrays.stream(URI.create(response.qrUrl()).getRawQuery().split("&"))
			.filter(parameter -> parameter.startsWith("des="))
			.findFirst().orElseThrow().substring(4);

		assertThat(response.paymentReference()).isEqualTo(persistedContent);
		assertThat(response.transferContent()).isEqualTo(persistedContent);
		assertThat(URLDecoder.decode(encodedDescription, StandardCharsets.UTF_8)).isEqualTo(persistedContent);
	}

	private SepayService service(String bankCode, String bankAccount, String bankAccountName) {
		return service(bankCode, bankAccount, bankAccountName, BANK_DESTINATION_SHA256);
	}

	private SepayService service(String bankCode, String bankAccount, String bankAccountName,
			String bankDestinationSha256) {
		return new SepayService(db, accounts, fulfillment,
			Clock.fixed(Instant.parse("2026-10-08T00:00:00Z"), ZoneOffset.UTC),
			bankCode, bankAccount, bankAccountName, bankDestinationSha256, "hook-secret");
	}
}
