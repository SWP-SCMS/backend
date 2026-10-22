package com.scms.backend.membership;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigInteger;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.account.MemberProfile;
import com.scms.backend.account.MemberProfileRepository;
import com.scms.backend.notification.NotificationOutboxDispatcher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@TestPropertySource(properties = {
	"sepay.bank-code=MB",
	"sepay.bank-account=123456789",
	"sepay.bank-account-name=SCMS GYM",
	"sepay.webhook-api-key=hook-secret"
})
class PaymentFlowIntegrationTests {
	@Container
	@ServiceConnection
	static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

	@Autowired MockMvc mockMvc;
	@MockitoSpyBean JdbcTemplate db;
	@MockitoBean MembershipLifecycleScheduler membershipLifecycleScheduler;
	@Autowired AccountRepository accounts;
	@Autowired MemberProfileRepository profiles;
	@Autowired JwtEncoder jwtEncoder;
	@Autowired PaymentService paymentService;
	@Autowired PaymentFulfillmentService fulfillment;
	@Autowired NotificationOutboxDispatcher notificationOutboxDispatcher;
	@Autowired Clock clock;
	@Autowired PlatformTransactionManager transactionManager;
	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void offerPatchIsPartialRejectsEmptyAndCannotChangePlan() throws Exception {
		Account manager = account(AccountRole.MANAGER);
		UUID offer = offer(manager, "BASIC", 12000, false);

		mockMvc.perform(patch("/api/v1/manager/membership-offers/{id}", offer).contextPath("/api/v1")
			.header(HttpHeaders.AUTHORIZATION, bearer(manager)).contentType(MediaType.APPLICATION_JSON)
			.content("{\"name\":\"Updated offer\"}"))
			.andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Updated offer"))
			.andExpect(jsonPath("$.priceAmount").value(12000)).andExpect(jsonPath("$.status").value("ACTIVE"));

		patchOffer(manager, offer, "{}").andExpect(status().isBadRequest());
		patchOffer(manager, offer, "{\"planCode\":\"PLUS\"}").andExpect(status().isBadRequest());
		patchOffer(manager, UUID.randomUUID(), "{\"name\":\"Missing\"}").andExpect(status().isNotFound());
	}

	@Test
	void managerCreatesOfferForActiveDynamicPlanAfterNormalizingCode() throws Exception {
		Account manager = account(AccountRole.MANAGER);
		insertPlan(" family ", "Family", "ACTIVE", true);

		mockMvc.perform(post("/api/v1/manager/membership-offers").contextPath("/api/v1")
			.header(HttpHeaders.AUTHORIZATION, bearer(manager)).contentType(MediaType.APPLICATION_JSON)
			.content("{\"planCode\":\" family \",\"name\":\"Family Offer\",\"description\":\"Family access\","
				+ "\"priceAmount\":700000,\"durationDays\":30}"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.planCode").value("FAMILY"));
	}

	@Test
	void managerRejectsOfferForUnknownPlanWithOfferValidationContract() throws Exception {
		Account manager = account(AccountRole.MANAGER);

		createOffer(manager, "UNKNOWN")
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("MEMBERSHIP_OFFER_VALIDATION_ERROR"));
	}

	@Test
	void managerRejectsOfferForInactivePlanWithOfferValidationContract() throws Exception {
		Account manager = account(AccountRole.MANAGER);
		insertPlan("INACTIVE_FAMILY", "Inactive Family", "INACTIVE", true);

		createOffer(manager, "INACTIVE_FAMILY")
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("MEMBERSHIP_OFFER_VALIDATION_ERROR"));
	}

	@Test
	void managerRejectsOfferForBenefitlessPlanWithOfferValidationContract() throws Exception {
		Account manager = account(AccountRole.MANAGER);
		insertPlan("NO_BENEFIT", "No Benefit", "ACTIVE", false);

		createOffer(manager, "NO_BENEFIT")
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("MEMBERSHIP_OFFER_VALIDATION_ERROR"));
	}

	@Test
	void cashConfirmationIsAtomicAndDoubleClickCannotFulfillTwice() throws Exception {
		Account receptionist = account(AccountRole.RECEPTIONIST);
		MemberProfile member = member();
		UUID offer = offer(receptionist, "BASIC", 12000, false);

		String response = cash(receptionist, member.getMemberCode(), offer).andExpect(status().isCreated())
			.andExpect(jsonPath("$.status").value("PAID")).andReturn().getResponse().getContentAsString();
		UUID payment = UUID.fromString(JsonPath.read(response, "$.paymentId"));
		UUID order = UUID.fromString(JsonPath.read(response, "$.orderId"));

		assertThat(count("select count(*) from payments where id=? and status='PAID'", payment)).isOne();
		assertThat(count("select count(*) from memberships where order_id=?", order)).isOne();
		assertThat(count("select count(*) from receipts where payment_id=?", payment)).isOne();
		assertThat(count("select count(*) from audit_events where target_id=?", payment)).isOne();
		notificationOutboxDispatcher.dispatchAvailable();
		assertThat(count("select count(*) from notifications where event_key=? and target_id=?",
			"PAYMENT_PAID:" + payment, payment)).isOne();

		cash(receptionist, member.getMemberCode(), offer).andExpect(status().isConflict());
		assertThat(count("select count(*) from memberships where member_account_id=?", member.getAccountId())).isOne();
	}

	@Test
	void cashRenewalReplaysByIdempotencyKeyWithoutCreatingDuplicateArtifacts() throws Exception {
		Account receptionist = account(AccountRole.RECEPTIONIST);
		MemberProfile member = member();
		UUID offer = offer(receptionist, "BASIC", 12000, false);
		cash(receptionist, member.getMemberCode(), offer).andExpect(status().isCreated());

		String firstRenewal = cash(receptionist, member.getMemberCode(), offer, "cash-renewal-1")
			.andExpect(status().isCreated()).andExpect(jsonPath("$.fulfillmentMode").value("RESERVED"))
			.andReturn().getResponse().getContentAsString();
		String replay = cash(receptionist, member.getMemberCode(), offer, "cash-renewal-1")
			.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();

		assertThat((String) JsonPath.read(replay, "$.paymentId"))
			.isEqualTo((String) JsonPath.read(firstRenewal, "$.paymentId"));
		assertThat(count("select count(*) from membership_orders where member_account_id=?", member.getAccountId())).isEqualTo(2);
		assertThat(count("select count(*) from payments p join membership_orders o on o.id=p.order_id where o.member_account_id=?",
			member.getAccountId())).isEqualTo(2);
		assertThat(count("select count(*) from receipts where member_account_id=?", member.getAccountId())).isEqualTo(2);
	}

	@Test
	void lateSepayRenewalActivatesFromTheExpiredMembershipEnd() throws Exception {
		Account manager = account(AccountRole.MANAGER);
		Account receptionist = account(AccountRole.RECEPTIONIST);
		MemberProfile member = member();
		UUID offer = offer(manager, "BASIC", 12000, false);
		cash(receptionist, member.getMemberCode(), offer).andExpect(status().isCreated());

		UUID renewalOrder = order(member.getAccountId(), member.getAccountId(), offer, 12000, false, null);
		Instant previousEnd = clock.instant().minusSeconds(3600);
		db.update("update membership_orders set fulfillment_mode='RESERVED',payment_method='BANK_TRANSFER' where id=?",
			renewalOrder);
		db.update("update memberships set ends_at=?,updated_at=current_timestamp where member_account_id=? and status='ACTIVE'",
			java.sql.Timestamp.from(previousEnd), member.getAccountId());

		JsonNode payment = objectMapper.readTree(sepay(member.getAccount(), renewalOrder)
			.andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
		String reference = payment.path("paymentReference").asText();
		UUID paymentId = UUID.fromString(payment.path("paymentId").asText());

		webhook("hook-secret", webhookBody(9104, reference, "PAY " + reference, 12000, "in", "123456789"))
			.andExpect(status().isOk()).andExpect(jsonPath("$.fulfillmentMode").value("RESERVED"));

		Map<String, Object> membership = db.queryForMap(
			"select status,starts_at,ends_at from memberships where order_id=?", renewalOrder);
		assertThat(membership.get("status")).isEqualTo("ACTIVE");
		assertThat(((java.sql.Timestamp) membership.get("starts_at")).toInstant()).isEqualTo(previousEnd);
		assertThat(((java.sql.Timestamp) membership.get("ends_at")).toInstant())
			.isEqualTo(previousEnd.plus(30, ChronoUnit.DAYS));
		assertThat(count("select count(*) from receipts where payment_id=?", paymentId)).isOne();
		assertThat(count("select count(*) from memberships where member_account_id=? and status='ACTIVE'",
			member.getAccountId())).isOne();
	}

	@Test
	void cashRejectsInactiveOfferAndInactivePlanWithOrderAvailabilityContract() throws Exception {
		Account receptionist = account(AccountRole.RECEPTIONIST);
		MemberProfile inactiveOfferMember = member();
		MemberProfile inactivePlanMember = member();
		UUID inactiveOffer = offer(receptionist, "BASIC", 12000, false);
		db.update("update membership_offers set status='INACTIVE' where id=?", inactiveOffer);
		insertPlan("DISABLED_CASH", "Disabled Cash", "INACTIVE", true);
		UUID inactivePlanOffer = offer(receptionist, "DISABLED_CASH", 12000, false);

		cash(receptionist, inactiveOfferMember.getMemberCode(), inactiveOffer)
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("MEMBERSHIP_OFFER_NOT_AVAILABLE"));
		cash(receptionist, inactivePlanMember.getMemberCode(), inactivePlanOffer)
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("MEMBERSHIP_OFFER_NOT_AVAILABLE"));
		assertThat(count("select count(*) from membership_orders where member_account_id in (?,?)",
			inactiveOfferMember.getAccountId(), inactivePlanMember.getAccountId())).isZero();
	}

	@Test
	void cashWaitsForPlanDeactivationAndThenReturnsUnavailable() throws Exception {
		Account receptionist = account(AccountRole.RECEPTIONIST);
		MemberProfile member = member();
		String planCode = "RACE_CASH_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
		insertPlan(planCode, "Race Cash", "ACTIVE", true);
		UUID offerId = offer(receptionist, planCode, 12000, false);
		CountDownLatch planLocked = new CountDownLatch(1);
		CountDownLatch allowCommit = new CountDownLatch(1);

		try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
			Future<?> deactivation = executor.submit(() -> new TransactionTemplate(transactionManager)
				.executeWithoutResult(status -> {
					db.update("update membership_plans set status='INACTIVE' where plan_code=?", planCode);
					planLocked.countDown();
					await(allowCommit);
				}));
			assertThat(planLocked.await(10, TimeUnit.SECONDS)).isTrue();
			Future<MvcResult> creation = executor.submit(() -> cash(receptionist, member.getMemberCode(), offerId)
				.andReturn());
			try {
				awaitPlanStatusLock();
			}
			finally {
				allowCommit.countDown();
			}
			deactivation.get(10, TimeUnit.SECONDS);
			MvcResult result = creation.get(10, TimeUnit.SECONDS);

			assertThat(result.getResponse().getStatus()).isEqualTo(409);
			assertThat(JsonPath.<String>read(result.getResponse().getContentAsString(), "$.code"))
				.isEqualTo("MEMBERSHIP_OFFER_NOT_AVAILABLE");
		}

		assertNoCommerceArtifacts(member.getAccountId());
	}

	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	void cashWaitsForOfferDeactivationAndThenReturnsUnavailable() throws Exception {
		Account receptionist = account(AccountRole.RECEPTIONIST);
		MemberProfile member = member();
		UUID offerId = offer(receptionist, "BASIC", 12000, false);
		CountDownLatch offerLocked = new CountDownLatch(1);
		CountDownLatch allowCommit = new CountDownLatch(1);

		try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
			Future<?> deactivation = executor.submit(() -> new TransactionTemplate(transactionManager)
				.executeWithoutResult(status -> {
					db.update("update membership_offers set status='INACTIVE' where id=?", offerId);
					offerLocked.countDown();
					await(allowCommit);
				}));
			assertThat(offerLocked.await(10, TimeUnit.SECONDS)).isTrue();
			Future<MvcResult> creation = executor.submit(() -> cash(receptionist, member.getMemberCode(), offerId)
				.andReturn());
			try {
				awaitOfferStatusLock();
			}
			finally {
				allowCommit.countDown();
			}
			deactivation.get(10, TimeUnit.SECONDS);
			MvcResult result = creation.get(10, TimeUnit.SECONDS);

			assertThat(result.getResponse().getStatus()).isEqualTo(409);
			assertThat(JsonPath.<String>read(result.getResponse().getContentAsString(), "$.code"))
				.isEqualTo("MEMBERSHIP_OFFER_NOT_AVAILABLE");
		}

		assertNoCommerceArtifacts(member.getAccountId());
	}

	@Test
	void notificationDeliveryFailureDoesNotRollBackPaidPaymentFulfillment() {
		Account receptionist = account(AccountRole.RECEPTIONIST);
		MemberProfile member = member();
		UUID offer = offer(receptionist, "BASIC", 12000, false);
		UUID order = order(member.getAccountId(), member.getAccountId(), offer, 12000, false,
			Instant.now().plusSeconds(3600));
		UUID payment = pendingPayment(order, 12000, "ROLLBACK-" + order);
		db.execute("create function fail_payment_notification() returns trigger language plpgsql as $$ begin "
			+ "raise exception 'forced notification failure'; end; $$");
		db.execute("create trigger fail_payment_notification before insert on notifications "
			+ "for each row execute function fail_payment_notification()");
		try {
			paymentService.reconcile(receptionist.getId(), payment,
				new PaymentActionRequest(PaymentActionRequest.ReconciliationStatus.PAID, BigInteger.valueOf(12000),
					"ROLLBACK-" + order, "rollback-transaction", "bank statement", "manual confirmation"));
			notificationOutboxDispatcher.dispatchAvailable();
		}
		finally {
			db.execute("drop trigger fail_payment_notification on notifications");
			db.execute("drop function fail_payment_notification()");
		}
		assertThat(db.queryForObject("select status from membership_orders where id=?", String.class, order))
			.isEqualTo("PAID");
		assertThat(db.queryForObject("select status from payments where id=?", String.class, payment))
			.isEqualTo("PAID");
		assertThat(count("select count(*) from memberships where member_account_id=?", member.getAccountId()))
			.isOne();
		assertThat(count("select count(*) from receipts where member_account_id=?", member.getAccountId()))
			.isOne();
		assertThat(count("select count(*) from audit_events where action='PAYMENT_PAID' and target_id=?",
			payment)).isOne();
		assertThat(count("select count(*) from notifications where event_key=?", "PAYMENT_PAID:" + payment))
			.isZero();
		assertThat(db.queryForObject("select status from notification_outbox where event_key=?", String.class,
			"PAYMENT_PAID:" + payment)).isEqualTo("FAILED");
	}

	@Test
	void sepayCreationReturnsManualFallbackWithoutLoadingQr() throws Exception {
		Account manager = account(AccountRole.MANAGER);
		MemberProfile member = member();
		UUID offer = offer(manager, "BASIC", 12000, false);
		UUID order = order(member.getAccountId(), member.getAccountId(), offer, 12000, false, null);

		String response = sepay(member.getAccount(), order).andExpect(status().isOk())
			.andExpect(jsonPath("$.amount").value(12000)).andExpect(jsonPath("$.status").value("PENDING"))
			.andReturn().getResponse().getContentAsString();
		JsonNode json = objectMapper.readTree(response);
		String persistedContent = db.queryForObject(
			"select bank_transfer_content from payments where order_id=?", String.class, order);

		assertThat(json.path("transferContent").asText()).isEqualTo(persistedContent);
		assertThat(json.path("paymentReference").asText()).isEqualTo(persistedContent);
		assertThat(json.path("bankCode").asText()).isEqualTo("MB");
		assertThat(json.path("bankAccountNumber").asText()).isEqualTo("123456789");
		assertThat(json.path("bankAccountName").asText()).isEqualTo("SCMS GYM");
		assertThat(json.path("currency").asText()).isEqualTo("VND");
		assertThat(json.path("expiresAt").asText()).isNotBlank();
		assertThat(response).doesNotContain("hook-secret", "webhookApiKey", "secretKey", "authorization", "credential");
		assertThat(db.queryForMap("""
			select bank_code_snapshot,bank_account_number_snapshot,bank_account_name_snapshot
			from payments where order_id=?
			""", order)).containsEntry("bank_code_snapshot", "MB")
			.containsEntry("bank_account_number_snapshot", "123456789")
			.containsEntry("bank_account_name_snapshot", "SCMS GYM");
	}

	@Test
	void sepayRetryReturnsTheSameFallbackSnapshotAndOnePayment() throws Exception {
		Account manager = account(AccountRole.MANAGER);
		MemberProfile member = member();
		UUID offer = offer(manager, "BASIC", 12000, false);
		UUID order = order(member.getAccountId(), member.getAccountId(), offer, 12000, false, null);

		JsonNode first = objectMapper.readTree(sepay(member.getAccount(), order).andExpect(status().isOk())
			.andReturn().getResponse().getContentAsString());
		JsonNode second = objectMapper.readTree(sepay(member.getAccount(), order).andExpect(status().isOk())
			.andReturn().getResponse().getContentAsString());

		assertThat(second).isEqualTo(first);
		assertThat(count("select count(*) from payments where order_id=? and status='PENDING'", order)).isOne();
	}

	@Test
	void sepayQrParametersMatchReturnedFallback() throws Exception {
		Account manager = account(AccountRole.MANAGER);
		MemberProfile member = member();
		UUID offer = offer(manager, "BASIC", 12000, false);
		UUID order = order(member.getAccountId(), member.getAccountId(), offer, 12000, false, null);

		JsonNode response = objectMapper.readTree(sepay(member.getAccount(), order).andExpect(status().isOk())
			.andReturn().getResponse().getContentAsString());
		Map<String, String> query = queryParameters(response.path("qrUrl").asText());

		assertThat(query).containsEntry("bank", response.path("bankCode").asText())
			.containsEntry("acc", response.path("bankAccountNumber").asText())
			.containsEntry("accountName", response.path("bankAccountName").asText())
			.containsEntry("amount", response.path("amount").asText())
			.containsEntry("des", response.path("transferContent").asText());
	}

	@Test
	void sepayDestinationSurvivesRestartAndConfigurationRotation() throws Exception {
		Account manager = account(AccountRole.MANAGER);
		MemberProfile member = member();
		UUID offer = offer(manager, "BASIC", 12000, false);
		UUID order = order(member.getAccountId(), member.getAccountId(), offer, 12000, false, null);
		JsonNode first = objectMapper.readTree(sepay(member.getAccount(), order).andExpect(status().isOk())
			.andReturn().getResponse().getContentAsString());
		SepayService rotated = new SepayService(db, accounts, fulfillment, clock,
			"VCB", "new-account", "NEW GYM", "hook-secret");

		SepayPaymentResponse retry = rotated.create(member.getAccountId(), order);

		assertThat(retry.paymentId().toString()).isEqualTo(first.path("paymentId").asText());
		assertThat(retry.bankCode()).isEqualTo("MB");
		assertThat(retry.bankAccountNumber()).isEqualTo("123456789");
		assertThat(retry.bankAccountName()).isEqualTo("SCMS GYM");
		assertThat(retry.qrUrl()).isEqualTo(first.path("qrUrl").asText());
		assertThat(count("select count(*) from payments where order_id=?", order)).isOne();
	}

	@Test
	void sepayRetryPreservesExactPersistedTransferContent() throws Exception {
		Account manager = account(AccountRole.MANAGER);
		MemberProfile member = member();
		UUID offer = offer(manager, "BASIC", 12000, false);
		UUID order = order(member.getAccountId(), member.getAccountId(), offer, 12000, false,
			Instant.now().plusSeconds(3600));
		String persistedContent = "  SCMS-EXACT CONTENT  ";
		pendingPayment(order, 12000, persistedContent);

		JsonNode response = objectMapper.readTree(sepay(member.getAccount(), order).andExpect(status().isOk())
			.andReturn().getResponse().getContentAsString());

		assertThat(response.path("paymentReference").asText()).isEqualTo(persistedContent);
		assertThat(response.path("transferContent").asText()).isEqualTo(persistedContent);
		assertThat(queryParameters(response.path("qrUrl").asText())).containsEntry("des", persistedContent);
		assertThat(db.queryForMap("""
			select bank_code_snapshot,bank_account_number_snapshot,bank_account_name_snapshot
			from payments where order_id=?
			""", order)).containsEntry("bank_code_snapshot", "MB")
			.containsEntry("bank_account_number_snapshot", "123456789")
			.containsEntry("bank_account_name_snapshot", "SCMS GYM");
	}

	@Test
	void sepayWebhookUsesPersistedAccountAfterConfigurationRotation() throws Exception {
		Account manager = account(AccountRole.MANAGER);
		MemberProfile member = member();
		UUID offer = offer(manager, "PLUS", 12000, false);
		UUID order = order(member.getAccountId(), member.getAccountId(), offer, 12000, false, null);
		JsonNode created = objectMapper.readTree(sepay(member.getAccount(), order).andExpect(status().isOk())
			.andReturn().getResponse().getContentAsString());
		SepayService rotated = new SepayService(db, accounts, fulfillment, clock,
			"VCB", "new-account", "NEW GYM", "hook-secret");
		String reference = created.path("paymentReference").asText();

		PaymentResultResponse result = rotated.webhook("hook-secret", new SepayWebhookRequest(
			9101L, reference, "PAY " + reference, BigInteger.valueOf(12000), "in", null, null, "123456789"));

		assertThat(result.status()).isEqualTo("PAID");
		assertThat(count("select count(*) from memberships where order_id=?", order)).isOne();
		assertThat(count("select count(*) from receipts where payment_id=?",
			UUID.fromString(created.path("paymentId").asText()))).isOne();
	}

	@Test
	void directLegacyWebhookFallsBackToCurrentConfiguredAccount() throws Exception {
		Account manager = account(AccountRole.MANAGER);
		MemberProfile member = member();
		UUID offer = offer(manager, "PLUS", 12000, false);
		UUID order = order(member.getAccountId(), member.getAccountId(), offer, 12000, false,
			Instant.now().plusSeconds(3600));
		String reference = "LEGACY-" + order;
		UUID payment = pendingPayment(order, 12000, reference);

		webhook("hook-secret", webhookBody(9102, reference, "PAY " + reference, 12000, "in", "123456789"))
			.andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PAID"));

		assertThat(count("select count(*) from receipts where payment_id=?", payment)).isOne();
	}

	@Test
	void legacyRetryRacingWebhookCompletesWithoutDeadlockOrDuplicateArtifacts() throws Exception {
		Account manager = account(AccountRole.MANAGER);
		MemberProfile member = member();
		UUID offer = offer(manager, "PLUS", 12000, false);
		UUID order = order(member.getAccountId(), member.getAccountId(), offer, 12000, false,
			Instant.now().plusSeconds(3600));
		String reference = "LEGACY-RACE-" + order;
		UUID payment = pendingPayment(order, 12000, reference);
		CountDownLatch start = new CountDownLatch(1);
		ExecutorService executor = Executors.newFixedThreadPool(2);

		try {
			Future<MvcResult> retry = executor.submit(() -> {
				if (!start.await(10, TimeUnit.SECONDS)) throw new AssertionError("Race did not start");
				return sepay(member.getAccount(), order).andReturn();
			});
			Future<MvcResult> webhook = executor.submit(() -> {
				if (!start.await(10, TimeUnit.SECONDS)) throw new AssertionError("Race did not start");
				return webhook("hook-secret",
					webhookBody(9103, reference, "PAY " + reference, 12000, "in", "123456789")).andReturn();
			});
			start.countDown();

			int retryStatus = retry.get(10, TimeUnit.SECONDS).getResponse().getStatus();
			assertThat(retryStatus).isIn(200, 409);
			assertThat(webhook.get(10, TimeUnit.SECONDS).getResponse().getStatus()).isEqualTo(200);
		}
		finally {
			start.countDown();
			executor.shutdownNow();
		}
		assertThat(count("select count(*) from memberships where order_id=?", order)).isOne();
		assertThat(count("select count(*) from receipts where payment_id=?", payment)).isOne();
	}

	@Test
	void sepayWebhookValidatesEveryTrustedFieldAndIsIdempotent() throws Exception {
		Account manager = account(AccountRole.MANAGER);
		MemberProfile member = member();
		UUID offer = offer(manager, "PLUS", 12000, false);
		UUID order = order(member.getAccountId(), member.getAccountId(), offer, 12000, false, null);
		String created = sepay(member.getAccount(), order).andReturn().getResponse().getContentAsString();
		String reference = JsonPath.read(created, "$.paymentReference");
		UUID payment = UUID.fromString(JsonPath.read(created, "$.paymentId"));

		webhook(null, webhookBody(9001, reference, "PAY " + reference, 12000, "in", "123456789"))
			.andExpect(status().isUnauthorized());
		webhook("hook-secret", webhookBody(9001, reference, "PAY " + reference, 12000, "out", "123456789"))
			.andExpect(status().isBadRequest());
		webhook("hook-secret", webhookBody(9001, reference, "WRONG", 12000, "in", "123456789"))
			.andExpect(status().isBadRequest());
		webhook("hook-secret", webhookBody(9001, reference, "PAY " + reference, 12001, "in", "123456789"))
			.andExpect(status().isBadRequest());
		webhook("hook-secret", webhookBody(9001, reference, "PAY " + reference, 12000, "in", "000"))
			.andExpect(status().isBadRequest());
		assertThat(count("select count(*) from memberships where order_id=?", order)).isZero();

		String paid = webhook("hook-secret", webhookBody(9001, reference, "PAY " + reference, 12000, "in", "123456789"))
			.andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PAID"))
			.andReturn().getResponse().getContentAsString();
		String receiptId = JsonPath.read(paid, "$.receiptId");
		webhook("hook-secret", webhookBody(9001, reference, "PAY " + reference, 12000, "in", "123456789"))
			.andExpect(status().isOk()).andExpect(jsonPath("$.receiptId").value(receiptId));
		webhook("hook-secret", webhookBody(9002, reference, "PAY " + reference, 12000, "in", "123456789"))
			.andExpect(status().isConflict());
		assertThat(count("select count(*) from memberships where order_id=?", order)).isOne();
		assertThat(count("select count(*) from receipts where payment_id=?", payment)).isOne();
	}

	@Test
	void providerTransactionCannotFulfillTwoPayments() throws Exception {
		Account manager = account(AccountRole.MANAGER);
		UUID offer = offer(manager, "BASIC", 12000, false);
		MemberProfile first = member();
		MemberProfile second = member();
		UUID firstOrder = order(first.getAccountId(), first.getAccountId(), offer, 12000, false, null);
		UUID secondOrder = order(second.getAccountId(), second.getAccountId(), offer, 12000, false, null);
		String firstRef = reference(sepay(first.getAccount(), firstOrder));
		String secondRef = reference(sepay(second.getAccount(), secondOrder));

		webhook("hook-secret", webhookBody(777, firstRef, firstRef, 12000, "in", "123456789"))
			.andExpect(status().isOk());
		webhook("hook-secret", webhookBody(777, secondRef, secondRef, 12000, "in", "123456789"))
			.andExpect(status().isConflict());
		assertThat(count("select count(*) from memberships where order_id=?", secondOrder)).isZero();
	}

	@Test
	void reconciliationHasStrictValidationAndTerminalStates() throws Exception {
		Account manager = account(AccountRole.MANAGER);
		Account receptionist = account(AccountRole.RECEPTIONIST);
		MemberProfile member = member();
		UUID offer = offer(manager, "BASIC", 12000, false);
		UUID order = order(member.getAccountId(), member.getAccountId(), offer, 12000, false,
			Instant.now().plusSeconds(3600));
		UUID payment = pendingPayment(order, 12000, "MANUAL-" + order);

		reconcile(receptionist, payment, "{\"status\":\"BANANA\",\"reason\":\"x\",\"evidence\":\"x\"}")
			.andExpect(status().isBadRequest());
		reconcile(receptionist, payment, paidReconciliation(12001, "MANUAL-" + order, "manual-1"))
			.andExpect(status().isBadRequest());
		reconcile(receptionist, payment, "{\"status\":\"FAILED\",\"reason\":\"too early\",\"evidence\":\"proof\"}")
			.andExpect(status().isConflict());
		reconcile(receptionist, payment, paidReconciliation(12000, "MANUAL-" + order, "manual-1"))
			.andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PAID"));
		reconcile(receptionist, payment, "{\"status\":\"FAILED\",\"reason\":\"late\",\"evidence\":\"proof\"}")
			.andExpect(status().isConflict());
		cancelOrder(manager, order, "Paid order cannot be cancelled").andExpect(status().isConflict());

		MemberProfile expiredMember = member();
		UUID expiredOrder = order(expiredMember.getAccountId(), expiredMember.getAccountId(), offer, 12000, false,
			Instant.now().minusSeconds(60));
		UUID expiredPayment = pendingPayment(expiredOrder, 12000, "EXPIRED-" + expiredOrder);
		reconcile(receptionist, expiredPayment,
			"{\"status\":\"FAILED\",\"reason\":\"window expired\",\"evidence\":\"bank lookup\"}")
			.andExpect(status().isOk()).andExpect(jsonPath("$.status").value("FAILED"));
		assertThat(db.queryForObject("select status from membership_orders where id=?", String.class, expiredOrder))
			.isEqualTo("EXPIRED");
	}

	@Test
	void reconciliationQueueFiltersSearchesPaginatesAndEnforcesRoles() throws Exception {
		Account manager = account(AccountRole.MANAGER);
		Account receptionist = account(AccountRole.RECEPTIONIST);
		Account coach = account(AccountRole.COACH);
		MemberProfile futureMember = member();
		MemberProfile expiredMember = member();
		MemberProfile noWindowMember = member();
		UUID offer = offer(manager, "BASIC", 12000, false);
		UUID futureOrder = order(futureMember.getAccountId(), futureMember.getAccountId(), offer, 12000, false,
			Instant.now().plusSeconds(3600));
		UUID expiredOrder = order(expiredMember.getAccountId(), expiredMember.getAccountId(), offer, 12000, false,
			Instant.now().minusSeconds(60));
		UUID noWindowOrder = order(noWindowMember.getAccountId(), noWindowMember.getAccountId(), offer, 12000,
			false, null);
		String marker = "QUEUE-" + UUID.randomUUID();
		UUID futurePayment = pendingPayment(futureOrder, 12000, marker + "-FUTURE");
		pendingPayment(expiredOrder, 12000, marker + "-EXPIRED");
		pendingPayment(noWindowOrder, 12000, marker + "-NO-WINDOW");

		getAuthorized(manager, "/api/v1/payments/reconciliation-queue?status=ALL&page=0&size=2&query=" + marker)
			.andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(2))
			.andExpect(jsonPath("$.totalElements").value(3)).andExpect(jsonPath("$.totalPages").value(2));
		getAuthorized(receptionist, "/api/v1/payments/reconciliation-queue?status=PENDING&query=" + marker)
			.andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2));
		getAuthorized(receptionist, "/api/v1/payments/reconciliation-queue?status=EXPIRED_WINDOW&query=" + marker)
			.andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
		getAuthorized(manager, "/api/v1/payments/reconciliation-queue?query=" + futurePayment)
			.andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
			.andExpect(jsonPath("$.content[0].paymentId").value(futurePayment.toString()));
		getAuthorized(coach, "/api/v1/payments/reconciliation-queue").andExpect(status().isForbidden());
		getAuthorized(futureMember.getAccount(), "/api/v1/payments/reconciliation-queue")
			.andExpect(status().isForbidden());
		getAuthorized(manager, "/api/v1/payments/reconciliation-queue?page=-1").andExpect(status().isBadRequest());
		getAuthorized(manager, "/api/v1/payments/reconciliation-queue?size=101").andExpect(status().isBadRequest());
		getAuthorized(manager, "/api/v1/payments/reconciliation-queue?status=UNKNOWN")
			.andExpect(status().isBadRequest());
	}

	@Test
	void managerAndReceptionistCanCancelOnlyPendingOrdersWithAudit() throws Exception {
		Account manager = account(AccountRole.MANAGER);
		Account receptionist = account(AccountRole.RECEPTIONIST);
		Account coach = account(AccountRole.COACH);
		MemberProfile member = member();
		UUID offer = offer(manager, "BASIC", 12000, false);
		UUID order = order(member.getAccountId(), member.getAccountId(), offer, 12000, false,
			Instant.now().plusSeconds(3600));
		UUID payment = pendingPayment(order, 12000, "CANCEL-" + order);

		cancelOrder(manager, order, "Customer requested cancellation")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.orderId").value(order.toString()))
			.andExpect(jsonPath("$.status").value("EXPIRED"))
			.andExpect(jsonPath("$.cancelledPayments").value(1));
		assertThat(db.queryForObject("select status from membership_orders where id=?", String.class, order))
			.isEqualTo("EXPIRED");
		assertThat(db.queryForObject("select status from payments where id=?", String.class, payment))
			.isEqualTo("FAILED");
		assertThat(count("select count(*) from audit_events where target_id=? and action='MEMBERSHIP_ORDER_CANCELLED'",
			order)).isOne();
		cancelOrder(manager, order, "Duplicate cancellation").andExpect(status().isConflict());

		MemberProfile secondMember = member();
		UUID secondOrder = order(secondMember.getAccountId(), secondMember.getAccountId(), offer, 12000, false, null);
		cancelOrder(receptionist, secondOrder, "Cancelled at the front desk")
			.andExpect(status().isOk()).andExpect(jsonPath("$.cancelledPayments").value(0));
		cancelOrder(coach, secondOrder, "Not allowed").andExpect(status().isForbidden());
		cancelOrder(member.getAccount(), secondOrder, "Not allowed").andExpect(status().isForbidden());
		cancelOrder(manager, UUID.randomUUID(), "Unknown order").andExpect(status().isNotFound());
		cancelOrder(manager, UUID.randomUUID(), " ").andExpect(status().isBadRequest());
	}

	@Test
	void receiptAndPaymentResultEnforceOwnershipAndRoleScope() throws Exception {
		Account receptionist = account(AccountRole.RECEPTIONIST);
		MemberProfile owner = member();
		MemberProfile stranger = member();
		UUID offer = offer(receptionist, "BASIC", 12000, false);
		String paid = cash(receptionist, owner.getMemberCode(), offer).andReturn().getResponse().getContentAsString();
		UUID payment = UUID.fromString(JsonPath.read(paid, "$.paymentId"));
		UUID receipt = UUID.fromString(JsonPath.read(paid, "$.receiptId"));

		mockMvc.perform(get("/api/v1/members/me/receipts").contextPath("/api/v1")
			.header(HttpHeaders.AUTHORIZATION, bearer(owner.getAccount())))
			.andExpect(status().isOk()).andExpect(jsonPath("$[0].receiptId").value(receipt.toString()))
			.andExpect(jsonPath("$[0].planCode").value("BASIC"));
		getAuthorized(owner.getAccount(), "/api/v1/receipts/" + receipt).andExpect(status().isOk());
		getAuthorized(owner.getAccount(), "/api/v1/payments/" + payment + "/result").andExpect(status().isOk());
		getAuthorized(stranger.getAccount(), "/api/v1/receipts/" + receipt).andExpect(status().isForbidden());
		getAuthorized(stranger.getAccount(), "/api/v1/payments/" + payment + "/result").andExpect(status().isForbidden());
		getAuthorized(receptionist, "/api/v1/receipts/" + receipt).andExpect(status().isOk())
			.andExpect(jsonPath("$.memberName").value(owner.getAccount().getFullName()))
			.andExpect(jsonPath("$.memberCode").value(owner.getMemberCode()));
	}

	@Test
	void webhookReceiptIsHiddenFromReceptionistButVisibleToManager() throws Exception {
		Account manager = account(AccountRole.MANAGER);
		Account receptionist = account(AccountRole.RECEPTIONIST);
		MemberProfile member = member();
		UUID offer = offer(manager, "BASIC", 12000, false);
		UUID order = order(member.getAccountId(), member.getAccountId(), offer, 12000, false, null);
		String created = sepay(member.getAccount(), order).andExpect(status().isOk())
			.andReturn().getResponse().getContentAsString();
		String reference = JsonPath.read(created, "$.paymentReference");
		UUID payment = UUID.fromString(JsonPath.read(created, "$.paymentId"));
		String paid = webhook("hook-secret",
			webhookBody(9100, reference, "PAY " + reference, 12000, "in", "123456789"))
			.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		UUID receipt = UUID.fromString(JsonPath.read(paid, "$.receiptId"));

		getAuthorized(receptionist, "/api/v1/receipts/" + receipt).andExpect(status().isForbidden());
		getAuthorized(manager, "/api/v1/receipts/" + receipt)
			.andExpect(status().isOk()).andExpect(jsonPath("$.paymentMethod").value("BANK_TRANSFER"));
		getAuthorized(receptionist, "/api/v1/payments/" + payment + "/result")
			.andExpect(status().isOk()).andExpect(jsonPath("$.method").value("BANK_TRANSFER"));
	}

	@Test
	void receptionistCanListCashAndBankTransferReceiptsForMember() throws Exception {
		Account manager = account(AccountRole.MANAGER);
		Account receptionist = account(AccountRole.RECEPTIONIST);
		Account coach = account(AccountRole.COACH);
		MemberProfile cashMember = member();
		MemberProfile bankMember = member();
		MemberProfile noReceiptMember = member();
		UUID offer = offer(manager, "BASIC", 12000, false);

		String cashPaid = cash(receptionist, cashMember.getMemberCode(), offer)
			.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		UUID cashReceipt = UUID.fromString(JsonPath.read(cashPaid, "$.receiptId"));
		getAuthorized(receptionist, "/api/v1/reception/members/" + cashMember.getMemberCode() + "/receipts")
			.andExpect(status().isOk()).andExpect(jsonPath("$[0].receiptId").value(cashReceipt.toString()))
			.andExpect(jsonPath("$[0].paymentMethod").value("CASH"))
			.andExpect(jsonPath("$[0].offerName").value("Offer " + offer))
			.andExpect(jsonPath("$[0].memberName").value(cashMember.getAccount().getFullName()))
			.andExpect(jsonPath("$[0].memberCode").value(cashMember.getMemberCode()));

		UUID bankOrder = order(bankMember.getAccountId(), bankMember.getAccountId(), offer, 12000, false, null);
		String created = sepay(bankMember.getAccount(), bankOrder).andExpect(status().isOk())
			.andReturn().getResponse().getContentAsString();
		String reference = JsonPath.read(created, "$.paymentReference");
		String bankPaid = webhook("hook-secret",
			webhookBody(9200, reference, "PAY " + reference, 12000, "in", "123456789"))
			.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		UUID bankReceipt = UUID.fromString(JsonPath.read(bankPaid, "$.receiptId"));
		db.update("update accounts set status='SUSPENDED' where id=?", bankMember.getAccountId());
		getAuthorized(receptionist, "/api/v1/reception/members/" + bankMember.getMemberCode() + "/receipts")
			.andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
		getAuthorized(manager, "/api/v1/reception/members/" + bankMember.getMemberCode() + "/receipts")
			.andExpect(status().isOk()).andExpect(jsonPath("$[0].receiptId").value(bankReceipt.toString()))
			.andExpect(jsonPath("$[0].paymentMethod").value("BANK_TRANSFER"));

		getAuthorized(receptionist, "/api/v1/reception/members/" + noReceiptMember.getMemberCode() + "/receipts")
			.andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
		getAuthorized(receptionist, "/api/v1/reception/members/MB-NOT-FOUND/receipts")
			.andExpect(status().isNotFound());
		getAuthorized(coach, "/api/v1/reception/members/" + cashMember.getMemberCode() + "/receipts")
			.andExpect(status().isForbidden());
		getAuthorized(manager, "/api/v1/reception/members/" + cashMember.getMemberCode() + "/receipts")
			.andExpect(status().isOk()).andExpect(jsonPath("$[0].offerName").value("Offer " + offer));
	}

	@Test
	void paymentEndpointsRejectAnonymousAndWrongRoles() throws Exception {
		Account coach = account(AccountRole.COACH);
		Account manager = account(AccountRole.MANAGER);
		MemberProfile member = member();
		UUID offer = offer(manager, "BASIC", 12000, false);

		mockMvc.perform(post("/api/v1/reception/members/{memberId}/cash-payments", member.getMemberCode())
			.contextPath("/api/v1").contentType(MediaType.APPLICATION_JSON)
			.content("{\"offerId\":\"" + offer + "\"}"))
			.andExpect(status().isUnauthorized());
		cash(coach, member.getMemberCode(), offer).andExpect(status().isForbidden());
		getAuthorized(coach, "/api/v1/members/me/receipts").andExpect(status().isForbidden());
		report(member.getAccount(), Instant.parse("2040-01-01T00:00:00Z"),
			Instant.parse("2040-02-01T00:00:00Z"), false).andExpect(status().isForbidden());
	}

	@Test
	void staffMembershipHistoryUsesOwnershipSnapshotsOrderingAndReconciliation() throws Exception {
		Account receptionist = account(AccountRole.RECEPTIONIST);
		Account manager = account(AccountRole.MANAGER);
		MemberProfile member = member();
		MemberProfile otherMember = member();
		UUID offer = offer(manager, "BASIC", 12000, false);
		UUID firstId = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");
		UUID secondId = UUID.fromString("00000000-0000-0000-0000-000000000001");
		Instant now = clock.instant();
		Instant startsAt = now.minusSeconds(172800).truncatedTo(ChronoUnit.MICROS);
		membershipHistoryEntry(member.getAccountId(), offer, firstId, "PLUS", "Snapshot Plus", "ACTIVE",
			startsAt, now.minusSeconds(60));
		membershipHistoryEntry(member.getAccountId(), offer, secondId, "BASIC", "Snapshot Basic", "EXPIRED",
			startsAt, now.plusSeconds(86400));
		UUID otherMembershipId = UUID.randomUUID();
		membershipHistoryEntry(otherMember.getAccountId(), offer, otherMembershipId, "BASIC", "Other Member",
			"ACTIVE", startsAt.plusSeconds(3600), now.minusSeconds(60));
		db.update("update membership_offers set name='Current Offer Name' where id=?", offer);

		getAuthorized(receptionist,
			"/api/v1/reception/members/" + member.getMemberCode() + "/memberships")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].membershipId").value(firstId.toString()))
			.andExpect(jsonPath("$[0].planCode").value("PLUS"))
			.andExpect(jsonPath("$[0].offerName").value("Snapshot Plus"))
			.andExpect(jsonPath("$[0].benefitCodes[0]").value("CENTER_ACCESS"))
			.andExpect(jsonPath("$[0].benefits[0].code").value("CENTER_ACCESS"))
			.andExpect(jsonPath("$[0].benefits[2].groupCode").value("PERSONAL_TRAINING"))
			.andExpect(jsonPath("$[0].benefits[2].groupDisplayOrder").value(60))
			.andExpect(jsonPath("$[0].status").value("EXPIRED"))
			.andExpect(jsonPath("$[0].startsAt").value(startsAt.toString()))
			.andExpect(jsonPath("$[1].membershipId").value(secondId.toString()))
			.andExpect(jsonPath("$[1].offerName").value("Snapshot Basic"))
			.andExpect(jsonPath("$.length()").value(2));
		assertThat(db.queryForObject("select status from memberships where id=?", String.class, firstId))
			.isEqualTo("EXPIRED");
		assertThat(db.queryForObject("select status from memberships where id=?", String.class, otherMembershipId))
			.isEqualTo("ACTIVE");

		getAuthorized(member.getAccount(), "/api/v1/members/me/memberships")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].benefitCodes[0]").value("CENTER_ACCESS"))
			.andExpect(jsonPath("$[0].benefits[0].code").value("CENTER_ACCESS"));

		db.update("update accounts set status='SUSPENDED' where id=?", member.getAccountId());
		getAuthorized(manager, "/api/v1/manager/members/" + member.getAccountId() + "/memberships")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].membershipId").value(firstId.toString()))
			.andExpect(jsonPath("$[1].membershipId").value(secondId.toString()));
	}

	@Test
	@SuppressWarnings("unchecked")
	void staffMembershipHistoryRollsBackScopedExpiryWhenHistoryQueryFails() {
		Account manager = account(AccountRole.MANAGER);
		MemberProfile member = member();
		UUID offer = offer(manager, "BASIC", 12000, false);
		UUID membershipId = UUID.randomUUID();
		Instant now = clock.instant();
		membershipHistoryEntry(member.getAccountId(), offer, membershipId, "BASIC", "Rollback Snapshot", "ACTIVE",
			now.minusSeconds(3600), now.minusSeconds(60));

		try {
			doThrow(new DataAccessResourceFailureException("forced history query failure")).when(db).query(
				argThat(sql -> sql.contains("from memberships where member_account_id=?")),
				org.mockito.ArgumentMatchers.<RowMapper<StaffMembershipResponse>>any(), eq(member.getAccountId()));

			assertThatThrownBy(() -> paymentService.managerMembershipHistory(manager.getId(), member.getAccountId()))
				.isInstanceOf(DataAccessResourceFailureException.class)
				.hasMessageContaining("forced history query failure");
		}
		finally {
			reset(db);
		}

		assertThat(db.queryForObject("select status from memberships where id=?", String.class, membershipId))
			.isEqualTo("ACTIVE");
	}

	@Test
	void staffMembershipHistoryEnforcesSecurityEmptyAndNotFoundContracts() throws Exception {
		Account receptionist = account(AccountRole.RECEPTIONIST);
		Account manager = account(AccountRole.MANAGER);
		Account coach = account(AccountRole.COACH);
		Account inactiveReceptionist = account(AccountRole.RECEPTIONIST);
		Account inactiveManager = account(AccountRole.MANAGER);
		MemberProfile member = member();
		String inactiveReceptionToken = bearer(inactiveReceptionist);
		String inactiveManagerToken = bearer(inactiveManager);
		db.update("update accounts set status='INACTIVE' where id in (?,?)", inactiveReceptionist.getId(),
			inactiveManager.getId());

		getAuthorized(receptionist,
			"/api/v1/reception/members/" + member.getMemberCode() + "/memberships")
			.andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
		getAuthorized(manager, "/api/v1/manager/members/" + member.getAccountId() + "/memberships")
			.andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
		getAuthorized(receptionist, "/api/v1/manager/members/" + member.getAccountId() + "/memberships")
			.andExpect(status().isForbidden());
		getAuthorized(manager, "/api/v1/reception/members/" + member.getMemberCode() + "/memberships")
			.andExpect(status().isForbidden());
		for (Account actor : List.of(member.getAccount(), coach)) {
			getAuthorized(actor, "/api/v1/reception/members/" + member.getMemberCode() + "/memberships")
				.andExpect(status().isForbidden());
			getAuthorized(actor, "/api/v1/manager/members/" + member.getAccountId() + "/memberships")
				.andExpect(status().isForbidden());
		}
		mockMvc.perform(get("/api/v1/reception/members/{memberCode}/memberships", member.getMemberCode())
			.contextPath("/api/v1").header(HttpHeaders.AUTHORIZATION, inactiveReceptionToken))
			.andExpect(status().isUnauthorized());
		mockMvc.perform(get("/api/v1/manager/members/{memberAccountId}/memberships", member.getAccountId())
			.contextPath("/api/v1").header(HttpHeaders.AUTHORIZATION, inactiveManagerToken))
			.andExpect(status().isUnauthorized());
		getAuthorized(receptionist, "/api/v1/reception/members/MB-999999/memberships")
			.andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("PAYMENT_NOT_FOUND"));
		getAuthorized(manager, "/api/v1/manager/members/" + UUID.randomUUID() + "/memberships")
			.andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("PAYMENT_NOT_FOUND"));
		getAuthorized(manager, "/api/v1/manager/members/" + coach.getId() + "/memberships")
			.andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("PAYMENT_NOT_FOUND"));
	}

	@Test
	void reportUsesHalfOpenBoundariesAndExcludesTestDataByDefault() throws Exception {
		Account manager = account(AccountRole.MANAGER);
		MemberProfile normal = member();
		MemberProfile test = member();
		db.update("update accounts set is_test_data=true where id=?", test.getAccountId());
		UUID offer = offer(manager, "BASIC", 12000, false);
		Instant from = Instant.parse("2040-01-01T00:00:00Z");
		Instant to = Instant.parse("2040-02-01T00:00:00Z");
		paidHistory(normal.getAccountId(), offer, 12000, from, false, "ACTIVE");
		paidHistory(test.getAccountId(), offer, 13000, from.plusSeconds(1), true, "EXPIRED");
		paidHistory(normal.getAccountId(), offer, 14000, to, false, null);

		String normalReport = report(manager, from, to, false).andExpect(status().isOk())
			.andExpect(jsonPath("$.paidPayments").value(1)).andExpect(jsonPath("$.paidRevenue").value(12000))
			.andReturn().getResponse().getContentAsString();
		String allReport = report(manager, from, to, true).andExpect(status().isOk())
			.andExpect(jsonPath("$.paidPayments").value(2)).andExpect(jsonPath("$.paidRevenue").value(25000))
			.andReturn().getResponse().getContentAsString();
		assertThat(JsonPath.<Integer>read(allReport, "$.expiredMemberships"))
			.isEqualTo(JsonPath.<Integer>read(normalReport, "$.expiredMemberships") + 1);
		report(manager, to, from, false).andExpect(status().isBadRequest());
	}

	@Test
	void migrationEnforcesSinglePendingPaymentTerminalImmutabilityAndTestFlagInheritance() {
		Account manager = account(AccountRole.MANAGER);
		MemberProfile member = member();
		db.update("update accounts set is_test_data=true where id=?", member.getAccountId());
		UUID offer = offer(manager, "BASIC", 12000, false);
		UUID order = order(member.getAccountId(), member.getAccountId(), offer, 12000, false, null);
		UUID payment = pendingPayment(order, 12000, "ONE-" + order);

		assertThat(db.queryForObject("select is_test_data from membership_orders where id=?", Boolean.class, order)).isTrue();
		assertThatThrownBy(() -> pendingPayment(order, 12000, "TWO-" + order))
			.isInstanceOf(DataIntegrityViolationException.class);
		db.update("update payments set status='FAILED',failure_reason='test' where id=?", payment);
		assertThatThrownBy(() -> db.update("update payments set status='PENDING' where id=?", payment))
			.isInstanceOf(org.springframework.dao.DataAccessException.class);
	}

	private Account account(AccountRole role) {
		UUID id = UUID.randomUUID();
		return accounts.saveAndFlush(new Account(id, role, AccountStatus.ACTIVE, role + " Payment Test",
			"09" + id.toString().replace("-", "").substring(0, 8), id + "@example.test", LocalDate.of(1990, 1, 1),
			"encoded-password"));
	}

	private MemberProfile member() {
		return profiles.saveAndFlush(new MemberProfile(account(AccountRole.MEMBER)));
	}

	private UUID offer(Account creator, String plan, long price, boolean testData) {
		UUID id = UUID.randomUUID();
		db.update("""
			insert into membership_offers(id,plan_code,name,description,price_amount,currency_code,duration_days,status,created_by_account_id)
			values(?,?,?,'Payment test offer',?,'VND',30,'ACTIVE',?)
			""", id, plan, (testData ? "QA Offer " : "Offer ") + id, price, creator.getId());
		return id;
	}

	private void insertPlan(String planCode, String displayName, String status, boolean withBenefit) {
		db.update("""
			insert into membership_plans(plan_code, display_name, supports_booking, supports_personal_coaching, status)
			values (?, ?, false, false, ?)
			""", planCode.trim().toUpperCase(), displayName, status);
		if (withBenefit) {
			db.update("insert into membership_plan_benefits(plan_code, benefit_code) values (?, 'CENTER_ACCESS')",
				planCode.trim().toUpperCase());
		}
	}

	private org.springframework.test.web.servlet.ResultActions createOffer(Account manager, String planCode)
			throws Exception {
		return mockMvc.perform(post("/api/v1/manager/membership-offers").contextPath("/api/v1")
			.header(HttpHeaders.AUTHORIZATION, bearer(manager)).contentType(MediaType.APPLICATION_JSON)
			.content("{\"planCode\":\"" + planCode + "\",\"name\":\"Offer\",\"description\":\"Access\","
				+ "\"priceAmount\":700000,\"durationDays\":30}"));
	}

	private UUID order(UUID member, UUID actor, UUID offer, long amount, boolean testData, Instant expiresAt) {
		UUID id = UUID.randomUUID();
		db.update("""
			insert into membership_orders(id,order_number,member_account_id,created_by_account_id,offer_id,
				offer_name_snapshot,plan_code_snapshot,price_amount_snapshot,currency_code_snapshot,
				duration_days_snapshot,payment_method,status,expires_at,is_test_data)
			values(?,?,?, ?,?,'Snapshot','BASIC',?,'VND',30,'BANK_TRANSFER','PENDING_PAYMENT',?,?)
			""", id, "ORD-" + id.toString().replace("-", "").toUpperCase(), member, actor, offer, amount,
			expiresAt == null ? null : java.sql.Timestamp.from(expiresAt), testData);
		return id;
	}

	private UUID pendingPayment(UUID order, long amount, String content) {
		UUID id = UUID.randomUUID();
		db.update("""
			insert into payments(id,order_id,method,status,amount,currency_code,bank_transfer_content,provider,provider_reference)
			values(?,?,'BANK_TRANSFER','PENDING',?,'VND',?,'SEPAY',?)
			""", id, order, amount, content, content);
		return id;
	}

	private void paidHistory(UUID member, UUID offer, long amount, Instant paidAt, boolean testData,
			String membershipStatus) {
		UUID order = UUID.randomUUID();
		UUID payment = UUID.randomUUID();
		db.update("""
			insert into membership_orders(id,order_number,member_account_id,created_by_account_id,offer_id,
				offer_name_snapshot,plan_code_snapshot,price_amount_snapshot,currency_code_snapshot,duration_days_snapshot,
				payment_method,status,expires_at,paid_at,created_at,updated_at,is_test_data)
			values(?,?,?, ?,?,'Report','BASIC',?,'VND',30,'BANK_TRANSFER','PAID',null,?,?,?,?)
			""", order, "ORD-" + order.toString().replace("-", "").toUpperCase(), member, member, offer, amount,
			java.sql.Timestamp.from(paidAt), java.sql.Timestamp.from(paidAt), java.sql.Timestamp.from(paidAt), testData);
		db.update("""
			insert into payments(id,order_id,method,status,amount,currency_code,bank_transfer_content,provider,provider_reference,
				provider_transaction_id,paid_at,created_at,updated_at)
			values(?,?,'BANK_TRANSFER','PAID',?,'VND',?,'SEPAY',?,?, ?,?,?)
			""", payment, order, amount, "REF-" + payment, "REF-" + payment, "TX-" + payment, java.sql.Timestamp.from(paidAt),
			java.sql.Timestamp.from(paidAt), java.sql.Timestamp.from(paidAt));
		if (membershipStatus != null) {
			db.update("""
				insert into memberships(id,member_account_id,order_id,offer_id,plan_code_snapshot,offer_name_snapshot,
					price_amount_snapshot,currency_code_snapshot,duration_days_snapshot,status,starts_at,ends_at)
				values(?,?,?,?, 'BASIC','Report',?,'VND',30,?,?,?)
				""", UUID.randomUUID(), member, order, offer, amount, membershipStatus,
				java.sql.Timestamp.from(paidAt), java.sql.Timestamp.from(paidAt.plusSeconds(2592000)));
		}
	}

	private void membershipHistoryEntry(UUID member, UUID offer, UUID membershipId, String planCode,
			String offerName, String status, Instant startsAt, Instant endsAt) {
		UUID order = UUID.randomUUID();
		UUID payment = UUID.randomUUID();
		db.update("""
			insert into membership_orders(id,order_number,member_account_id,created_by_account_id,offer_id,
				offer_name_snapshot,plan_code_snapshot,price_amount_snapshot,currency_code_snapshot,
				duration_days_snapshot,payment_method,status,expires_at,paid_at)
			values(?,?,?,?,?,?,?,12000,'VND',30,'CASH','PAID',null,?)
			""", order, "ORD-" + order.toString().replace("-", "").toUpperCase(), member, member, offer,
			offerName, planCode, java.sql.Timestamp.from(startsAt));
		db.update("""
			insert into payments(id,order_id,method,status,amount,currency_code,processed_by_account_id,paid_at)
			values(?,?,'CASH','PAID',12000,'VND',?,?)
			""", payment, order, member, java.sql.Timestamp.from(startsAt));
		db.update("""
			insert into memberships(id,member_account_id,order_id,offer_id,plan_code_snapshot,offer_name_snapshot,
				price_amount_snapshot,currency_code_snapshot,duration_days_snapshot,status,starts_at,ends_at)
			values(?,?,?,?,?,?,12000,'VND',30,?,?,?)
			""", membershipId, member, order, offer, planCode, offerName, status,
			java.sql.Timestamp.from(startsAt), java.sql.Timestamp.from(endsAt));
	}

	private org.springframework.test.web.servlet.ResultActions cash(Account actor, String memberCode, UUID offer)
			throws Exception {
		return cash(actor, memberCode, offer, null);
	}

	private org.springframework.test.web.servlet.ResultActions cash(Account actor, String memberCode, UUID offer,
		String idempotencyKey) throws Exception {
		var request = post("/api/v1/reception/members/{memberId}/cash-payments", memberCode)
			.contextPath("/api/v1").header(HttpHeaders.AUTHORIZATION, bearer(actor))
			.contentType(MediaType.APPLICATION_JSON).content("{\"offerId\":\"" + offer + "\"}");
		if (idempotencyKey != null) request.header("Idempotency-Key", idempotencyKey);
		return mockMvc.perform(request);
	}

	private org.springframework.test.web.servlet.ResultActions patchOffer(Account actor, UUID offer, String body)
			throws Exception {
		return mockMvc.perform(patch("/api/v1/manager/membership-offers/{id}", offer).contextPath("/api/v1")
			.header(HttpHeaders.AUTHORIZATION, bearer(actor)).contentType(MediaType.APPLICATION_JSON).content(body));
	}

	private org.springframework.test.web.servlet.ResultActions sepay(Account member, UUID order) throws Exception {
		return mockMvc.perform(post("/api/v1/members/me/membership-orders/{id}/payments/sepay", order)
			.contextPath("/api/v1").header(HttpHeaders.AUTHORIZATION, bearer(member)));
	}

	private org.springframework.test.web.servlet.ResultActions webhook(String key, String body) throws Exception {
		var request = post("/api/v1/payments/sepay/webhook").contextPath("/api/v1")
			.contentType(MediaType.APPLICATION_JSON).content(body);
		if (key != null) request.header(HttpHeaders.AUTHORIZATION, "Apikey " + key);
		return mockMvc.perform(request);
	}

	private org.springframework.test.web.servlet.ResultActions reconcile(Account actor, UUID payment, String body)
			throws Exception {
		return mockMvc.perform(post("/api/v1/payments/{id}/reconcile", payment).contextPath("/api/v1")
			.header(HttpHeaders.AUTHORIZATION, bearer(actor)).contentType(MediaType.APPLICATION_JSON).content(body));
	}

	private org.springframework.test.web.servlet.ResultActions cancelOrder(Account actor, UUID order, String reason)
			throws Exception {
		return mockMvc.perform(patch("/api/v1/membership-orders/{id}/cancel", order).contextPath("/api/v1")
			.header(HttpHeaders.AUTHORIZATION, bearer(actor)).contentType(MediaType.APPLICATION_JSON)
			.content("{\"reason\":\"" + reason + "\"}"));
	}

	private org.springframework.test.web.servlet.ResultActions getAuthorized(Account actor, String path)
			throws Exception {
		return mockMvc.perform(get(path).contextPath("/api/v1").header(HttpHeaders.AUTHORIZATION, bearer(actor)));
	}

	private org.springframework.test.web.servlet.ResultActions report(Account manager, Instant from, Instant to,
			boolean includeTestData) throws Exception {
		return mockMvc.perform(get("/api/v1/manager/reports/membership-revenue").contextPath("/api/v1")
			.header(HttpHeaders.AUTHORIZATION, bearer(manager)).queryParam("from", from.toString())
			.queryParam("to", to.toString()).queryParam("includeTestData", String.valueOf(includeTestData)));
	}

	private String reference(org.springframework.test.web.servlet.ResultActions action) throws Exception {
		return JsonPath.read(action.andExpect(status().isOk()).andReturn().getResponse().getContentAsString(),
			"$.paymentReference");
	}

	private String webhookBody(long id, String code, String content, long amount, String type, String account) {
		return """
			{"id":%d,"code":"%s","content":"%s","transferAmount":%d,"transferType":"%s","accountNumber":"%s"}
			""".formatted(id, code, content, amount, type, account);
	}

	private String paidReconciliation(long amount, String content, String transaction) {
		return """
			{"status":"PAID","receivedAmount":%d,"transferContent":"%s","providerTransactionId":"%s","evidence":"bank statement","reason":"manual confirmation"}
			""".formatted(amount, content, transaction);
	}

	private long count(String sql, Object... args) {
		return db.queryForObject(sql, Long.class, args);
	}

	private void awaitPlanStatusLock() throws InterruptedException {
		for (int attempt = 0; attempt < 200; attempt++) {
			Long waiters = db.queryForObject("""
				select count(*) from pg_stat_activity
				where datname=current_database() and pid<>pg_backend_pid()
					and wait_event_type='Lock' and query ilike '%membership_plans%'
				""", Long.class);
			if (waiters != null && waiters > 0) return;
			Thread.sleep(25);
		}
		throw new AssertionError("Cash creation did not wait on the membership plan lock");
	}

	private void awaitOfferStatusLock() throws InterruptedException {
		for (int attempt = 0; attempt < 200; attempt++) {
			Long waiters = db.queryForObject("""
				select count(*) from pg_stat_activity
				where datname=current_database() and pid<>pg_backend_pid()
					and wait_event_type='Lock' and query ilike '%membership_offers%'
				""", Long.class);
			if (waiters != null && waiters > 0) return;
			Thread.sleep(25);
		}
		throw new AssertionError("Cash creation did not wait on the membership offer lock");
	}

	private void await(CountDownLatch latch) {
		try {
			if (!latch.await(10, TimeUnit.SECONDS)) throw new AssertionError("Timed out waiting to commit plan status");
		}
		catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new AssertionError(exception);
		}
	}

	private void assertNoCommerceArtifacts(UUID memberAccountId) {
		assertThat(count("select count(*) from membership_orders where member_account_id=?", memberAccountId)).isZero();
		assertThat(count("""
			select count(*) from payments p join membership_orders o on o.id=p.order_id
			where o.member_account_id=?
			""", memberAccountId)).isZero();
		assertThat(count("select count(*) from memberships where member_account_id=?", memberAccountId)).isZero();
		assertThat(count("select count(*) from receipts where member_account_id=?", memberAccountId)).isZero();
	}

	private Map<String, String> queryParameters(String url) {
		return Arrays.stream(URI.create(url).getRawQuery().split("&"))
			.map(parameter -> parameter.split("=", 2))
			.collect(Collectors.toMap(parameter -> decode(parameter[0]), parameter -> decode(parameter[1])));
	}

	private String decode(String value) {
		return URLDecoder.decode(value, StandardCharsets.UTF_8);
	}

	private String bearer(Account account) {
		Instant now = Instant.now();
		String token = jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),
			JwtClaimsSet.builder().subject(account.getId().toString()).issuedAt(now).expiresAt(now.plusSeconds(900))
				.claim("role", account.getRole().name()).build())).getTokenValue();
		return "Bearer " + token;
	}
}
