package com.scms.backend.membership;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

import com.jayway.jsonpath.JsonPath;
import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.account.MemberProfile;
import com.scms.backend.account.MemberProfileRepository;
import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class MembershipOrderIntegrationTests {

	private static final BigInteger EXACT_PRICE = new BigInteger("9000000000000000001");
	private static final String PASSWORD = "unused-password";

	static {
		TimeZone.setDefault(TimeZone.getTimeZone(ZoneOffset.UTC));
	}

	@Container
	@ServiceConnection
	static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AccountRepository accountRepository;

	@Autowired
	private MemberProfileRepository memberProfileRepository;

	@Autowired
	private MembershipOrderRepository membershipOrderRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private JwtEncoder jwtEncoder;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private EntityManager entityManager;

	@Test
	void activeMemberCreatesBankTransferOrderWithSnapshotAndNoOutOfScopeSideEffects() throws Exception {
		MemberProfile member = createMember(AccountStatus.ACTIVE);
		UUID offerId = insertOffer(member.getAccount(), "PLUS", "Snapshot Offer", EXACT_PRICE, 45, "ACTIVE");
		Map<String, Long> before = outOfScopeCounts();

		MvcResult result = createForMember(accessToken(member.getAccount()), offerId)
			.andExpect(status().isCreated())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.memberId").value(member.getMemberCode()))
			.andExpect(jsonPath("$.offerId").value(offerId.toString()))
			.andExpect(jsonPath("$.offerName").value("Snapshot Offer"))
			.andExpect(jsonPath("$.planCode").value("PLUS"))
			.andExpect(jsonPath("$.priceAmount").isNumber())
			.andExpect(jsonPath("$.currencyCode").value("VND"))
			.andExpect(jsonPath("$.durationDays").value(45))
			.andExpect(jsonPath("$.paymentMethod").value("BANK_TRANSFER"))
			.andExpect(jsonPath("$.status").value("PENDING_PAYMENT"))
			.andExpect(jsonPath("$.createdAt").exists())
			.andExpect(jsonPath("$.expiresAt").doesNotExist())
			.andReturn();

		String body = result.getResponse().getContentAsString();
		Map<String, Object> response = JsonPath.read(body, "$");
		assertThat(response.keySet()).containsExactlyInAnyOrder("orderId", "orderNumber", "memberId", "offerId",
			"offerName", "planCode", "priceAmount", "currencyCode", "durationDays", "paymentMethod", "status",
			"createdAt");
		assertThat(response.get("priceAmount").toString()).isEqualTo(EXACT_PRICE.toString());
		assertThat(response.get("orderNumber").toString()).matches("ORD-[0-9A-F]{32}");
		assertThat(body).doesNotContain("password", "token", "version", "createdByAccountId", "expiresAt");

		MembershipOrder saved = membershipOrderRepository.findById(UUID.fromString(response.get("orderId").toString()))
			.orElseThrow();
		assertThat(saved.getMemberAccountId()).isEqualTo(member.getAccountId());
		assertThat(saved.getCreatedByAccountId()).isEqualTo(member.getAccountId());
		assertThat(saved.getOfferId()).isEqualTo(offerId);
		assertThat(saved.getOfferNameSnapshot()).isEqualTo("Snapshot Offer");
		assertThat(saved.getPlanCodeSnapshot()).isEqualTo(MembershipPlanCode.PLUS);
		assertThat(saved.getPriceAmountSnapshot()).isEqualTo(EXACT_PRICE);
		assertThat(saved.getCurrencyCodeSnapshot()).isEqualTo("VND");
		assertThat(saved.getDurationDaysSnapshot()).isEqualTo(45);
		assertThat(saved.getPaymentMethod()).isEqualTo(PaymentMethod.BANK_TRANSFER);
		assertThat(saved.getStatus()).isEqualTo(MembershipOrderStatus.PENDING_PAYMENT);
		assertThat(saved.getExpiresAt()).isNull();
		assertThat(saved.getPaidAt()).isNull();
		assertThat(saved.getCreatedAt()).isNotNull();
		assertOutOfScopeCountsUnchanged(before);
	}

	@Test
	void activeReceptionistCreatesForTargetAndPersistsDistinctActorAndMember() throws Exception {
		Account receptionist = createAccount(AccountRole.RECEPTIONIST, AccountStatus.ACTIVE);
		MemberProfile member = createMember(AccountStatus.ACTIVE);
		UUID offerId = insertOffer(receptionist, "BASIC", "Reception Offer", BigInteger.valueOf(500000), 30,
			"ACTIVE");

		MvcResult result = createForReceptionist(accessToken(receptionist), member.getMemberCode(), offerId)
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.memberId").value(member.getMemberCode()))
			.andExpect(jsonPath("$.paymentMethod").value("BANK_TRANSFER"))
			.andReturn();

		UUID orderId = UUID.fromString(JsonPath.read(result.getResponse().getContentAsString(), "$.orderId"));
		MembershipOrder saved = membershipOrderRepository.findById(orderId).orElseThrow();
		assertThat(saved.getCreatedByAccountId()).isEqualTo(receptionist.getId());
		assertThat(saved.getMemberAccountId()).isEqualTo(member.getAccountId());
	}

	@Test
	void twoMembersReceiveUniqueWellFormedOrderNumbers() throws Exception {
		Account creator = createAccount(AccountRole.RECEPTIONIST, AccountStatus.ACTIVE);
		MemberProfile first = createMember(AccountStatus.ACTIVE);
		MemberProfile second = createMember(AccountStatus.ACTIVE);
		UUID offerId = insertOffer(creator, "BASIC", "Shared Offer", BigInteger.valueOf(500000), 30, "ACTIVE");

		String firstNumber = JsonPath.read(createForMember(accessToken(first.getAccount()), offerId)
			.andReturn().getResponse().getContentAsString(), "$.orderNumber");
		String secondNumber = JsonPath.read(createForMember(accessToken(second.getAccount()), offerId)
			.andReturn().getResponse().getContentAsString(), "$.orderNumber");

		assertThat(firstNumber).matches("ORD-[0-9A-F]{32}").isNotEqualTo(secondNumber);
		assertThat(secondNumber).matches("ORD-[0-9A-F]{32}");
	}

	@Test
	void memberAndReceptionistResumePendingOrderAndSnapshotSurvivesOfferUpdate() throws Exception {
		Account receptionist = createAccount(AccountRole.RECEPTIONIST, AccountStatus.ACTIVE);
		MemberProfile member = createMember(AccountStatus.ACTIVE);
		UUID offerId = insertOffer(receptionist, "BASIC", "Original Offer", BigInteger.valueOf(500000), 30,
			"ACTIVE");
		createForReceptionist(accessToken(receptionist), member.getMemberCode(), offerId)
			.andExpect(status().isCreated());
		jdbcTemplate.update("update membership_offers set name = 'Changed Offer', price_amount = 800000, "
			+ "duration_days = 90 where id = ?", offerId);
		entityManager.clear();

		resumeForMember(accessToken(member.getAccount()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.offerName").value("Original Offer"))
			.andExpect(jsonPath("$.priceAmount").value(500000))
			.andExpect(jsonPath("$.durationDays").value(30));
		resumeForReceptionist(accessToken(receptionist), member.getMemberCode())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.offerName").value("Original Offer"));
	}

	@Test
	void resumeWithoutPendingOrderReturnsEmpty204() throws Exception {
		MemberProfile member = createMember(AccountStatus.ACTIVE);
		Account receptionist = createAccount(AccountRole.RECEPTIONIST, AccountStatus.ACTIVE);

		resumeForMember(accessToken(member.getAccount()))
			.andExpect(status().isNoContent())
			.andExpect(content().string(""));
		resumeForReceptionist(accessToken(receptionist), member.getMemberCode())
			.andExpect(status().isNoContent())
			.andExpect(content().string(""));
	}

	@Test
	void nonexistentAndInactiveOffersUseSameNotFoundContract() throws Exception {
		MemberProfile member = createMember(AccountStatus.ACTIVE);
		UUID inactiveOffer = insertOffer(member.getAccount(), "BASIC", "Inactive", BigInteger.valueOf(100000),
			10, "INACTIVE");

		createForMember(accessToken(member.getAccount()), UUID.randomUUID())
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("MEMBERSHIP_OFFER_NOT_FOUND"));
		createForMember(accessToken(member.getAccount()), inactiveOffer)
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("MEMBERSHIP_OFFER_NOT_FOUND"));
	}

	@Test
	void missingAndNonMemberReceptionTargetsAreNotFound() throws Exception {
		Account receptionist = createAccount(AccountRole.RECEPTIONIST, AccountStatus.ACTIVE);
		UUID offerId = insertOffer(receptionist, "BASIC", "Target Offer", BigInteger.valueOf(100000), 10,
			"ACTIVE");
		Account coach = createAccount(AccountRole.COACH, AccountStatus.ACTIVE);
		MemberProfile invalidProfile = memberProfileRepository.saveAndFlush(new MemberProfile(coach));

		createForReceptionist(accessToken(receptionist), "MB-999999999", offerId)
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("MEMBER_NOT_FOUND"));
		createForReceptionist(accessToken(receptionist), invalidProfile.getMemberCode(), offerId)
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("MEMBER_NOT_FOUND"));
	}

	@Test
	void suspendedReceptionTargetReturnsMemberNotActiveConflict() throws Exception {
		Account receptionist = createAccount(AccountRole.RECEPTIONIST, AccountStatus.ACTIVE);
		MemberProfile member = createMember(AccountStatus.SUSPENDED);
		UUID offerId = insertOffer(receptionist, "BASIC", "Target Offer", BigInteger.valueOf(100000), 10,
			"ACTIVE");

		createForReceptionist(accessToken(receptionist), member.getMemberCode(), offerId)
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("MEMBER_NOT_ACTIVE"));
	}

	@Test
	void inactiveReceptionTargetReturnsMemberNotActiveConflict() throws Exception {
		Account receptionist = createAccount(AccountRole.RECEPTIONIST, AccountStatus.ACTIVE);
		MemberProfile member = createMember(AccountStatus.ACTIVE);
		UUID offerId = insertOffer(receptionist, "BASIC", "Target Offer", BigInteger.valueOf(100000), 10,
			"ACTIVE");
		dropAccountRoleLifecycleConstraint();
		jdbcTemplate.update("update accounts set status = 'INACTIVE' where id = ?", member.getAccountId());
		entityManager.clear();

		createForReceptionist(accessToken(receptionist), member.getMemberCode(), offerId)
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("MEMBER_NOT_ACTIVE"));
	}

	@Test
	void activeMembershipBlocksNewOrder() throws Exception {
		MemberProfile member = createMember(AccountStatus.ACTIVE);
		UUID offerId = insertOffer(member.getAccount(), "PLUS", "Membership Offer", BigInteger.valueOf(700000),
			30, "ACTIVE");
		insertActiveMembership(member.getAccount(), offerId);

		createForMember(accessToken(member.getAccount()), offerId)
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("ACTIVE_MEMBERSHIP_EXISTS"));
	}

	@Test
	void existingPendingOrderIsRejectedByPrecheck() throws Exception {
		MemberProfile member = createMember(AccountStatus.ACTIVE);
		UUID offerId = insertOffer(member.getAccount(), "BASIC", "Pending Offer", BigInteger.valueOf(400000), 20,
			"ACTIVE");
		createForMember(accessToken(member.getAccount()), offerId).andExpect(status().isCreated());

		createForMember(accessToken(member.getAccount()), offerId)
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("PENDING_MEMBERSHIP_ORDER_EXISTS"));
		assertThat(membershipOrderRepository.count()).isEqualTo(1);
	}

	@Test
	void databaseUniqueIndexStillRejectsDuplicatePendingOrder() {
		MemberProfile member = createMember(AccountStatus.ACTIVE);
		UUID offerId = insertOffer(member.getAccount(), "BASIC", "Index Offer", BigInteger.valueOf(400000), 20,
			"ACTIVE");
		insertPendingOrder(member.getAccount().getId(), member.getAccount().getId(), offerId);

		assertThatThrownBy(() -> insertPendingOrder(member.getAccount().getId(), member.getAccount().getId(), offerId))
			.isInstanceOf(DataIntegrityViolationException.class)
			.hasMessageContaining(MembershipOrderConstraintDetector.PENDING_MEMBER_CONSTRAINT);
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"{}",
		"{\"offerId\":null}"
	})
	void missingOrNullOfferIdReturnsValidationError(String body) throws Exception {
		MemberProfile member = createMember(AccountStatus.ACTIVE);

		createForMember(accessToken(member.getAccount()), body)
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
			.andExpect(jsonPath("$.errors.offerId").exists());
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"{\"offerId\":\"not-a-uuid\"}",
		"{\"offerId\":",
		"{\"offerId\":\"00000000-0000-0000-0000-000000000001\",\"paymentMethod\":\"CASH\"}",
		"{\"offerId\":\"00000000-0000-0000-0000-000000000001\",\"memberId\":\"MB-1\"}",
		"{\"offerId\":\"00000000-0000-0000-0000-000000000001\",\"priceAmount\":1}"
	})
	void malformedUnknownAndReadOnlyFieldsReturnMalformedRequest(String body) throws Exception {
		MemberProfile member = createMember(AccountStatus.ACTIVE);

		createForMember(accessToken(member.getAccount()), body)
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
	}

	@Test
	void missingInvalidExpiredAndInactiveActorTokensAreUnauthorized() throws Exception {
		MemberProfile member = createMember(AccountStatus.ACTIVE);
		UUID offerId = insertOffer(member.getAccount(), "BASIC", "Auth Offer", BigInteger.valueOf(200000), 10,
			"ACTIVE");

		createForMember(null, offerId).andExpect(status().isUnauthorized());
		createForMember("not-a-jwt", offerId).andExpect(status().isUnauthorized());
		createForMember(accessToken(member.getAccount(), Instant.now().minusSeconds(60)), offerId)
			.andExpect(status().isUnauthorized());
		String token = accessToken(member.getAccount());
		jdbcTemplate.update("update accounts set status = 'SUSPENDED' where id = ?", member.getAccountId());
		entityManager.clear();
		createForMember(token, offerId).andExpect(status().isUnauthorized());
	}

	@ParameterizedTest
	@EnumSource(value = AccountRole.class, names = {"COACH", "MANAGER", "RECEPTIONIST"})
	void nonMembersCannotUseMemberSelfEndpoints(AccountRole role) throws Exception {
		Account actor = createAccount(role, AccountStatus.ACTIVE);

		createForMember(accessToken(actor), UUID.randomUUID()).andExpect(status().isForbidden());
		resumeForMember(accessToken(actor)).andExpect(status().isForbidden());
	}

	@ParameterizedTest
	@EnumSource(value = AccountRole.class, names = {"MEMBER", "COACH", "MANAGER"})
	void nonReceptionistsCannotUseReceptionistEndpoints(AccountRole role) throws Exception {
		Account actor = createAccount(role, AccountStatus.ACTIVE);

		createForReceptionist(accessToken(actor), "MB-999999999", UUID.randomUUID())
			.andExpect(status().isForbidden());
		resumeForReceptionist(accessToken(actor), "MB-999999999").andExpect(status().isForbidden());
	}

	private Account createAccount(AccountRole role, AccountStatus status) {
		UUID id = UUID.randomUUID();
		return accountRepository.saveAndFlush(new Account(id, role, status, role + " Order Test",
			"07" + id.toString().replace("-", "").substring(0, 8), id + "@example.test",
			LocalDate.of(1990, 1, 1), passwordEncoder.encode(PASSWORD)));
	}

	private MemberProfile createMember(AccountStatus status) {
		Account account = createAccount(AccountRole.MEMBER, status);
		return memberProfileRepository.saveAndFlush(new MemberProfile(account));
	}

	private UUID insertOffer(Account creator, String planCode, String name, BigInteger price, int durationDays,
			String status) {
		UUID id = UUID.randomUUID();
		jdbcTemplate.update("""
			insert into membership_offers
			    (id, plan_code, name, description, price_amount, currency_code, duration_days, status,
			     created_by_account_id)
			values (?, ?, ?, 'Order test offer', ?, 'VND', ?, ?, ?)
			""", id, planCode, name, price, durationDays, status, creator.getId());
		return id;
	}

	private void insertPendingOrder(UUID memberAccountId, UUID actorAccountId, UUID offerId) {
		UUID orderId = UUID.randomUUID();
		jdbcTemplate.update("""
			insert into membership_orders
			    (id, order_number, member_account_id, created_by_account_id, offer_id, offer_name_snapshot,
			     plan_code_snapshot, price_amount_snapshot, currency_code_snapshot, duration_days_snapshot,
			     payment_method, status, expires_at, paid_at)
			values (?, ?, ?, ?, ?, 'Index Offer', 'BASIC', 400000, 'VND', 20,
			        'BANK_TRANSFER', 'PENDING_PAYMENT', null, null)
			""", orderId, orderNumber(orderId), memberAccountId, actorAccountId, offerId);
	}

	private void insertActiveMembership(Account member, UUID offerId) {
		UUID orderId = UUID.randomUUID();
		jdbcTemplate.update("""
			insert into membership_orders
			    (id, order_number, member_account_id, created_by_account_id, offer_id, offer_name_snapshot,
			     plan_code_snapshot, price_amount_snapshot, currency_code_snapshot, duration_days_snapshot,
			     payment_method, status, expires_at, paid_at)
			values (?, ?, ?, ?, ?, 'Membership Offer', 'PLUS', 700000, 'VND', 30,
			        'BANK_TRANSFER', 'PAID', current_timestamp + interval '1 day', current_timestamp)
			""", orderId, orderNumber(orderId), member.getId(), member.getId(), offerId);
		jdbcTemplate.update("""
			insert into payments
			    (id, order_id, method, status, amount, currency_code, bank_transfer_content,
			     provider_reference, processed_by_account_id, paid_at)
			values (?, ?, 'BANK_TRANSFER', 'PAID', 700000, 'VND', ?, ?, ?, current_timestamp)
			""", UUID.randomUUID(), orderId, "PAY " + orderId, "PROVIDER-" + orderId, member.getId());
		jdbcTemplate.update("""
			insert into memberships
			    (id, member_account_id, order_id, offer_id, plan_code_snapshot, offer_name_snapshot,
			     price_amount_snapshot, currency_code_snapshot, duration_days_snapshot, status, starts_at, ends_at)
			values (?, ?, ?, ?, 'PLUS', 'Membership Offer', 700000, 'VND', 30, 'ACTIVE',
			        current_timestamp - interval '1 day', current_timestamp + interval '29 days')
			""", UUID.randomUUID(), member.getId(), orderId, offerId);
	}

	private Map<String, Long> outOfScopeCounts() {
		return Map.of("payments", count("payments"), "memberships", count("memberships"),
			"receipts", count("receipts"), "notifications", count("notifications"),
			"audit_events", count("audit_events"));
	}

	private void assertOutOfScopeCountsUnchanged(Map<String, Long> before) {
		before.forEach((table, expected) -> assertThat(count(table)).as(table).isEqualTo(expected));
	}

	private long count(String table) {
		return jdbcTemplate.queryForObject("select count(*) from " + table, Long.class);
	}

	private String orderNumber(UUID orderId) {
		return "ORD-" + orderId.toString().replace("-", "").toUpperCase();
	}

	private String accessToken(Account account) {
		return accessToken(account, Instant.now().plusSeconds(900));
	}

	private String accessToken(Account account, Instant expiresAt) {
		Instant now = Instant.now();
		JwtClaimsSet claims = JwtClaimsSet.builder()
			.subject(account.getId().toString())
			.issuedAt(expiresAt.isAfter(now) ? now : expiresAt.minusSeconds(900))
			.expiresAt(expiresAt)
			.claim("role", account.getRole().name())
			.build();
		return jwtEncoder.encode(JwtEncoderParameters.from(
			JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
	}

	private ResultActions createForMember(String token, UUID offerId) throws Exception {
		return createForMember(token, "{\"offerId\":\"" + offerId + "\"}");
	}

	private ResultActions createForMember(String token, String body) throws Exception {
		var request = post("/api/v1/members/me/membership-orders")
			.contextPath("/api/v1")
			.contentType(MediaType.APPLICATION_JSON)
			.content(body);
		if (token != null) {
			request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
		}
		return mockMvc.perform(request);
	}

	private ResultActions resumeForMember(String token) throws Exception {
		return mockMvc.perform(get("/api/v1/members/me/membership-orders/pending")
			.contextPath("/api/v1")
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
	}

	private ResultActions createForReceptionist(String token, String memberId, UUID offerId) throws Exception {
		return mockMvc.perform(post(receptionPath(memberId))
			.contextPath("/api/v1")
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"offerId\":\"" + offerId + "\"}"));
	}

	private ResultActions resumeForReceptionist(String token, String memberId) throws Exception {
		return mockMvc.perform(get(receptionPath(memberId) + "/pending")
			.contextPath("/api/v1")
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
	}

	private String receptionPath(String memberId) {
		return "/api/v1/reception/members/" + memberId + "/membership-orders";
	}

	private void dropAccountRoleLifecycleConstraint() {
		jdbcTemplate.execute("alter table accounts drop constraint chk_accounts_role_lifecycle");
	}
}
