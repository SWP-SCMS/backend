package com.scms.backend.membership;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import com.scms.backend.audit.AuditEventRepository;
import com.scms.backend.notification.NotificationRepository;
import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
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
class MembershipOfferDetailIntegrationTests {

	private static final String OFFER_PATH = "/api/v1/membership-offers/";
	private static final BigInteger EXACT_PRICE = new BigInteger("9000000000000000001");

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
	private AuditEventRepository auditEventRepository;

	@Autowired
	private NotificationRepository notificationRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private JwtEncoder jwtEncoder;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private EntityManager entityManager;

	@Test
	void activeMemberGetsActiveOfferWithExactPublicFieldsAndNoWrites() throws Exception {
		Account member = createAccount(AccountRole.MEMBER, AccountStatus.ACTIVE);
		UUID offerId = UUID.randomUUID();
		insertOffer(offerId, "PLUS", "Exact Price Offer", "Detail description", EXACT_PRICE, 90, "ACTIVE",
			member);
		long offerCount = count("membership_offers");
		long auditCount = auditEventRepository.count();
		long notificationCount = notificationRepository.count();

		MvcResult result = getOffer(accessToken(member), offerId.toString())
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.offerId").value(offerId.toString()))
			.andExpect(jsonPath("$.planCode").value("PLUS"))
			.andExpect(jsonPath("$.planDisplayName").value("Plus"))
			.andExpect(jsonPath("$.name").value("Exact Price Offer"))
			.andExpect(jsonPath("$.description").value("Detail description"))
			.andExpect(jsonPath("$.priceAmount").isNumber())
			.andExpect(jsonPath("$.currencyCode").value("VND"))
			.andExpect(jsonPath("$.durationDays").value(90))
			.andExpect(jsonPath("$.supportsBooking").value(true))
			.andExpect(jsonPath("$.supportsPersonalCoaching").value(true))
			.andReturn();

		String body = result.getResponse().getContentAsString();
		Map<String, Object> response = JsonPath.read(body, "$");
		assertThat(response.keySet()).containsExactlyInAnyOrder("offerId", "planCode", "planDisplayName", "name",
			"description", "priceAmount", "currencyCode", "durationDays", "supportsBooking",
			"supportsPersonalCoaching");
		assertThat(response.get("priceAmount").toString()).isEqualTo(EXACT_PRICE.toString());
		assertThat(body).contains("\"priceAmount\":" + EXACT_PRICE).doesNotContain("status", "createdByAccountId",
			"createdAt", "updatedAt", "version");
		assertThat(count("membership_offers")).isEqualTo(offerCount);
		assertThat(auditEventRepository.count()).isEqualTo(auditCount);
		assertThat(notificationRepository.count()).isEqualTo(notificationCount);
	}

	@Test
	void activeReceptionistCanGetActiveOffer() throws Exception {
		Account receptionist = createAccount(AccountRole.RECEPTIONIST, AccountStatus.ACTIVE);
		UUID offerId = UUID.randomUUID();
		insertOffer(offerId, "BASIC", "Reception Offer", "Available", BigInteger.valueOf(500000), 30,
			"ACTIVE", receptionist);

		getOffer(accessToken(receptionist), offerId.toString())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.offerId").value(offerId.toString()));
	}

	@ParameterizedTest
	@EnumSource(value = AccountRole.class, names = {"COACH", "MANAGER"})
	void anyAuthenticatedRoleCanReadPublicOfferDetail(AccountRole role) throws Exception {
		Account account = createAccount(role, AccountStatus.ACTIVE);

		getOffer(accessToken(account), UUID.randomUUID().toString()).andExpect(status().isNotFound());
	}

	@Test
	void anonymousAndInvalidBearerTokensHaveDifferentContracts() throws Exception {
		mockMvc.perform(get(OFFER_PATH + UUID.randomUUID()).contextPath("/api/v1"))
			.andExpect(status().isNotFound());
		getOffer("not-a-jwt", UUID.randomUUID().toString()).andExpect(status().isUnauthorized());
	}

	@Test
	void expiredBearerTokenIsUnauthorized() throws Exception {
		Account member = createAccount(AccountRole.MEMBER, AccountStatus.ACTIVE);

		getOffer(accessToken(member, Instant.now().minusSeconds(300)), UUID.randomUUID().toString())
			.andExpect(status().isUnauthorized());
	}

	@Test
	void tokenIsRejectedAfterAccountStopsBeingActive() throws Exception {
		Account member = createAccount(AccountRole.MEMBER, AccountStatus.ACTIVE);
		String token = accessToken(member);
		jdbcTemplate.update("update accounts set status = 'SUSPENDED' where id = ?", member.getId());
		entityManager.clear();

		getOffer(token, UUID.randomUUID().toString()).andExpect(status().isUnauthorized());
	}

	@Test
	void tokenIsRejectedAfterAccountNoLongerExists() throws Exception {
		Account nonexistentMember = new Account(UUID.randomUUID(), AccountRole.MEMBER, AccountStatus.ACTIVE,
			"Nonexistent Member", "0800000000", "missing@example.test", LocalDate.of(1990, 1, 1),
			passwordEncoder.encode("unused-password"));

		getOffer(accessToken(nonexistentMember), UUID.randomUUID().toString()).andExpect(status().isUnauthorized());
	}

	@Test
	void nonexistentAndInactiveOffersHaveSameNotFoundContract() throws Exception {
		Account member = createAccount(AccountRole.MEMBER, AccountStatus.ACTIVE);
		UUID inactiveOfferId = UUID.randomUUID();
		insertOffer(inactiveOfferId, "BASIC", "Hidden Offer", "Inactive", BigInteger.valueOf(100000), 10,
			"INACTIVE", member);

		String nonexistentBody = getOffer(accessToken(member), UUID.randomUUID().toString())
			.andExpect(status().isNotFound())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.code").value("MEMBERSHIP_OFFER_NOT_FOUND"))
			.andReturn().getResponse().getContentAsString();
		String inactiveBody = getOffer(accessToken(member), inactiveOfferId.toString())
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("MEMBERSHIP_OFFER_NOT_FOUND"))
			.andReturn().getResponse().getContentAsString();

		assertThat(JsonPath.<String>read(inactiveBody, "$.title"))
			.isEqualTo(JsonPath.read(nonexistentBody, "$.title"));
		assertThat(JsonPath.<String>read(inactiveBody, "$.detail"))
			.isEqualTo(JsonPath.read(nonexistentBody, "$.detail"));
	}

	@Test
	void malformedUuidReturnsSharedMalformedRequestProblem() throws Exception {
		Account member = createAccount(AccountRole.MEMBER, AccountStatus.ACTIVE);

		getOffer(accessToken(member), "not-a-uuid")
			.andExpect(status().isBadRequest())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"))
			.andExpect(jsonPath("$.status").value(400));
	}

	@Test
	void planDisplayAndCapabilitiesComeFromMembershipPlansTable() throws Exception {
		Account member = createAccount(AccountRole.MEMBER, AccountStatus.ACTIVE);
		UUID offerId = UUID.randomUUID();
		jdbcTemplate.update("""
			update membership_plans
			set display_name = 'Plus from database', supports_booking = false,
			    supports_personal_coaching = false
			where plan_code = 'PLUS'
			""");
		insertOffer(offerId, "PLUS", "Database Plan Offer", "Plan source proof", BigInteger.valueOf(800000),
			30, "ACTIVE", member);

		getOffer(accessToken(member), offerId.toString())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.planDisplayName").value("Plus from database"))
			.andExpect(jsonPath("$.supportsBooking").value(false))
			.andExpect(jsonPath("$.supportsPersonalCoaching").value(false));
	}

	@Test
	void memberWithActiveMembershipCanStillGetOfferDetail() throws Exception {
		Account member = createAccount(AccountRole.MEMBER, AccountStatus.ACTIVE);
		UUID offerId = UUID.randomUUID();
		insertOffer(offerId, "PLUS", "Current Offer", "Current", BigInteger.valueOf(700000), 30, "ACTIVE",
			member);
		insertActiveMembership(member, offerId);

		getOffer(accessToken(member), offerId.toString())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.offerId").value(offerId.toString()));
	}

	@Test
	void us11ListEndpointKeepsItsContract() throws Exception {
		Account member = createAccount(AccountRole.MEMBER, AccountStatus.ACTIVE);
		UUID activeOfferId = UUID.randomUUID();
		insertOffer(activeOfferId, "BASIC", "List Still Works", "Active", BigInteger.valueOf(400000), 30,
			"ACTIVE", member);
		insertOffer(UUID.randomUUID(), "PLUS", "List Hidden", "Inactive", BigInteger.valueOf(900000), 30,
			"INACTIVE", member);

		mockMvc.perform(get("/api/v1/membership-offers")
			.contextPath("/api/v1")
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken(member)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(1))
			.andExpect(jsonPath("$[0].offerId").value(activeOfferId.toString()))
			.andExpect(jsonPath("$[0].status").doesNotExist());
	}

	private Account createAccount(AccountRole role, AccountStatus status) {
		UUID id = UUID.randomUUID();
		return accountRepository.saveAndFlush(new Account(id, role, status, role.name() + " Detail Test",
			"08" + id.toString().replace("-", "").substring(0, 8), id + "@example.test",
			LocalDate.of(1990, 1, 1), passwordEncoder.encode("unused-password")));
	}

	private void insertOffer(UUID id, String planCode, String name, String description, BigInteger priceAmount,
			int durationDays, String status, Account creator) {
		jdbcTemplate.update("""
			insert into membership_offers
			    (id, plan_code, name, description, price_amount, currency_code, duration_days, status,
			     created_by_account_id)
			values (?, ?, ?, ?, ?, 'VND', ?, ?, ?)
			""", id, planCode, name, description, priceAmount, durationDays, status, creator.getId());
	}

	private void insertActiveMembership(Account member, UUID offerId) {
		UUID orderId = UUID.randomUUID();
		jdbcTemplate.update("""
			insert into membership_orders
			    (id, order_number, member_account_id, created_by_account_id, offer_id, offer_name_snapshot,
			     plan_code_snapshot, price_amount_snapshot, currency_code_snapshot, duration_days_snapshot,
			     payment_method, status, expires_at, paid_at)
			values (?, ?, ?, ?, ?, 'Current Offer', 'PLUS', 700000, 'VND', 30,
			        'BANK_TRANSFER', 'PAID', current_timestamp + interval '1 day', current_timestamp)
			""", orderId, "ORD-" + orderId.toString().replace("-", ""), member.getId(), member.getId(), offerId);
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
			values (?, ?, ?, ?, 'PLUS', 'Current Offer', 700000, 'VND', 30, 'ACTIVE',
			        current_timestamp - interval '1 day', current_timestamp + interval '29 days')
			""", UUID.randomUUID(), member.getId(), orderId, offerId);
	}

	private long count(String table) {
		return jdbcTemplate.queryForObject("select count(*) from " + table, Long.class);
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

	private ResultActions getOffer(String accessToken, String offerId) throws Exception {
		return mockMvc.perform(get(OFFER_PATH + offerId)
			.contextPath("/api/v1")
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken));
	}
}
