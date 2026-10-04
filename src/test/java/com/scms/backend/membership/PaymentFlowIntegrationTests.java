package com.scms.backend.membership;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import com.jayway.jsonpath.JsonPath;
import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.account.MemberProfile;
import com.scms.backend.account.MemberProfileRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
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
	@Autowired JdbcTemplate db;
	@Autowired AccountRepository accounts;
	@Autowired MemberProfileRepository profiles;
	@Autowired JwtEncoder jwtEncoder;

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
		assertThat(count("select count(*) from notifications where target_id=?", payment)).isOne();

		cash(receptionist, member.getMemberCode(), offer).andExpect(status().isConflict());
		assertThat(count("select count(*) from memberships where member_account_id=?", member.getAccountId())).isOne();
	}

	@Test
	void sepayCreationIsIdempotentAndQrUsesSnapshot() throws Exception {
		Account manager = account(AccountRole.MANAGER);
		MemberProfile member = member();
		UUID offer = offer(manager, "BASIC", 12000, false);
		UUID order = order(member.getAccountId(), member.getAccountId(), offer, 12000, false, null);

		String first = sepay(member.getAccount(), order).andExpect(status().isOk())
			.andExpect(jsonPath("$.amount").value(12000)).andExpect(jsonPath("$.status").value("PENDING"))
			.andReturn().getResponse().getContentAsString();
		String second = sepay(member.getAccount(), order).andExpect(status().isOk()).andReturn()
			.getResponse().getContentAsString();

		assertThat(JsonPath.<String>read(second, "$.paymentId")).isEqualTo(JsonPath.read(first, "$.paymentId"));
		assertThat(JsonPath.<String>read(first, "$.qrUrl")).contains("amount=12000", "acc=123456789");
		assertThat(count("select count(*) from payments where order_id=? and status='PENDING'", order)).isOne();
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
		reconcile(receptionist, payment, paidReconciliation(12000, "MANUAL-" + order, "manual-1"))
			.andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PAID"));
		reconcile(receptionist, payment, "{\"status\":\"FAILED\",\"reason\":\"late\",\"evidence\":\"proof\"}")
			.andExpect(status().isConflict());

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
			.andExpect(status().isOk()).andExpect(jsonPath("$[0].receiptId").value(receipt.toString()));
		getAuthorized(owner.getAccount(), "/api/v1/receipts/" + receipt).andExpect(status().isOk());
		getAuthorized(owner.getAccount(), "/api/v1/payments/" + payment + "/result").andExpect(status().isOk());
		getAuthorized(stranger.getAccount(), "/api/v1/receipts/" + receipt).andExpect(status().isForbidden());
		getAuthorized(stranger.getAccount(), "/api/v1/payments/" + payment + "/result").andExpect(status().isForbidden());
		getAuthorized(receptionist, "/api/v1/receipts/" + receipt).andExpect(status().isOk());
	}

	@Test
	void receptionistCanReadBankTransferReceiptAndPaymentResult() throws Exception {
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

		getAuthorized(receptionist, "/api/v1/receipts/" + receipt)
			.andExpect(status().isOk()).andExpect(jsonPath("$.paymentMethod").value("BANK_TRANSFER"));
		getAuthorized(receptionist, "/api/v1/payments/" + payment + "/result")
			.andExpect(status().isOk()).andExpect(jsonPath("$.method").value("BANK_TRANSFER"));
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

	private org.springframework.test.web.servlet.ResultActions cash(Account actor, String memberCode, UUID offer)
			throws Exception {
		return mockMvc.perform(post("/api/v1/reception/members/{memberId}/cash-payments", memberCode)
			.contextPath("/api/v1").header(HttpHeaders.AUTHORIZATION, bearer(actor))
			.contentType(MediaType.APPLICATION_JSON).content("{\"offerId\":\"" + offer + "\"}"));
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

	private String bearer(Account account) {
		Instant now = Instant.now();
		String token = jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),
			JwtClaimsSet.builder().subject(account.getId().toString()).issuedAt(now).expiresAt(now.plusSeconds(900))
				.claim("role", account.getRole().name()).build())).getTokenValue();
		return "Bearer " + token;
	}
}
