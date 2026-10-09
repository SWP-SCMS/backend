package com.scms.backend.membership;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class SepayServiceTests {
	@Mock JdbcTemplate db;
	@Mock AccountRepository accounts;
	@Mock PaymentFulfillmentService fulfillment;

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
	void firstLegacyRetrySnapshotsCurrentDestinationOnce() {
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
		when(db.update(any(String.class), any(Object[].class))).thenAnswer(invocation -> {
			payment.put("bank_code_snapshot", "MB");
			payment.put("bank_account_number_snapshot", "123456789");
			payment.put("bank_account_name_snapshot", "SCMS GYM");
			return 1;
		});
		AtomicReference<SepayPaymentResponse> first = new AtomicReference<>();

		assertThatCode(() -> first.set(service("MB", "123456789", "SCMS GYM").create(actor, orderId)))
			.doesNotThrowAnyException();
		SepayPaymentResponse second = service("VCB", "new-account", "NEW GYM").create(actor, orderId);

		assertThat(first.get().bankCode()).isEqualTo("MB");
		assertThat(second.bankCode()).isEqualTo("MB");
		assertThat(second.bankAccountNumber()).isEqualTo("123456789");
		assertThat(second.bankAccountName()).isEqualTo("SCMS GYM");
		verify(db, times(1)).update(any(String.class), any(Object[].class));
	}

	@Test
	void blankAccountNameRejectsLegacySnapshotBeforeWrite() {
		UUID actor = UUID.randomUUID();
		UUID orderId = UUID.randomUUID();
		Map<String, Object> payment = new HashMap<>();
		payment.put("id", UUID.randomUUID());
		payment.put("order_id", orderId);
		payment.put("bank_code_snapshot", null);
		payment.put("bank_account_number_snapshot", null);
		payment.put("bank_account_name_snapshot", null);
		when(accounts.existsByIdAndRoleAndStatus(actor, AccountRole.MEMBER, AccountStatus.ACTIVE)).thenReturn(true);
		when(db.queryForMap("select * from membership_orders where id=? and member_account_id=? for update",
			orderId, actor)).thenReturn(Map.of(
				"status", "PENDING_PAYMENT", "expires_at", Timestamp.from(Instant.parse("2026-10-09T00:00:00Z"))));
		when(db.queryForList(
			"select * from payments where order_id=? and status='PENDING' order by created_at limit 1", orderId))
			.thenReturn(List.of(payment));

		assertThatThrownBy(() -> service("MB", "123456789", " ").create(actor, orderId))
			.isInstanceOf(PaymentException.class)
			.hasMessage("SePay bank configuration is missing");
		verify(db, never()).update(any(String.class), any(Object[].class));
	}

	@Test
	void webhookUsesPersistedAccountAfterConfigurationRotation() {
		UUID paymentId = UUID.randomUUID();
		UUID orderId = UUID.randomUUID();
		Map<String, Object> payment = Map.of(
			"bank_transfer_content", "SCMS-ORD-123", "payment_amount", BigDecimal.valueOf(12000),
			"bank_account_number_snapshot", "old-account");
		PaymentResultResponse expected = new PaymentResultResponse(
			paymentId, orderId, "PAID", "BANK_TRANSFER", UUID.randomUUID(), UUID.randomUUID(),
			Instant.parse("2026-10-08T00:00:00Z"));
		when(db.queryForObject("select id from payments where provider='SEPAY' and provider_reference=?",
			UUID.class, "SCMS-ORD-123")).thenReturn(paymentId);
		when(fulfillment.paymentForUpdate(paymentId)).thenReturn(payment);
		when(fulfillment.fulfill(paymentId, null, "9001", "SePay webhook transaction 9001",
			"SePay incoming transfer confirmed")).thenReturn(expected);
		SepayWebhookRequest request = new SepayWebhookRequest(9001L, "SCMS-ORD-123", "PAY SCMS-ORD-123",
			BigInteger.valueOf(12000), "in", null, null, "old-account");
		AtomicReference<PaymentResultResponse> result = new AtomicReference<>();

		assertThatCode(() -> result.set(service("MB", "new-account", "NEW GYM")
			.webhook("hook-secret", request))).doesNotThrowAnyException();

		assertThat(result.get()).isSameAs(expected);
	}

	@Test
	void directLegacyWebhookFallsBackToCurrentConfiguredAccount() {
		UUID paymentId = UUID.randomUUID();
		UUID orderId = UUID.randomUUID();
		Map<String, Object> payment = new HashMap<>();
		payment.put("bank_transfer_content", "SCMS-ORD-123");
		payment.put("payment_amount", BigDecimal.valueOf(12000));
		payment.put("bank_account_number_snapshot", null);
		PaymentResultResponse expected = new PaymentResultResponse(
			paymentId, orderId, "PAID", "BANK_TRANSFER", UUID.randomUUID(), UUID.randomUUID(),
			Instant.parse("2026-10-08T00:00:00Z"));
		when(db.queryForObject("select id from payments where provider='SEPAY' and provider_reference=?",
			UUID.class, "SCMS-ORD-123")).thenReturn(paymentId);
		when(fulfillment.paymentForUpdate(paymentId)).thenReturn(payment);
		when(fulfillment.fulfill(paymentId, null, "9001", "SePay webhook transaction 9001",
			"SePay incoming transfer confirmed")).thenReturn(expected);
		SepayWebhookRequest request = new SepayWebhookRequest(9001L, "SCMS-ORD-123", "PAY SCMS-ORD-123",
			BigInteger.valueOf(12000), "in", null, null, "123456789");

		assertThat(service("MB", "123456789", "SCMS GYM").webhook("hook-secret", request)).isSameAs(expected);
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
		return new SepayService(db, accounts, fulfillment,
			Clock.fixed(Instant.parse("2026-10-08T00:00:00Z"), ZoneOffset.UTC),
			bankCode, bankAccount, bankAccountName, "hook-secret");
	}
}
