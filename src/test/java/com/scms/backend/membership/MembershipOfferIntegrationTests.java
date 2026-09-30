package com.scms.backend.membership;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
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
import org.junit.jupiter.params.provider.ValueSource;
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
class MembershipOfferIntegrationTests {

	private static final String PASSWORD = "membership-offer-password";
	private static final String LOGIN_PATH = "/api/v1/auth/login";
	private static final String OFFER_PATH = "/api/v1/membership-offers";

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
	void activeMemberGetsAllAndOnlyActiveOffersWithExactPublicFields() throws Exception {
		Account member = createAccount(AccountRole.MEMBER, AccountStatus.ACTIVE);
		UUID basicOfferId = UUID.randomUUID();
		UUID plusOfferId = UUID.randomUUID();
		insertOffer(basicOfferId, "BASIC", "Basic 30 Days", "Basic description", 500000, 30, "ACTIVE", member);
		insertOffer(plusOfferId, "PLUS", "Plus 60 Days", "Plus description", 900000, 60, "ACTIVE", member);
		insertOffer(UUID.randomUUID(), "BASIC", "Hidden Offer", "Inactive", 100000, 10, "INACTIVE", member);
		long offerCount = countOffers();
		long auditCount = auditEventRepository.count();
		long notificationCount = notificationRepository.count();

		MvcResult result = getOffers(accessToken(member), null)
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$[0].offerId").value(basicOfferId.toString()))
			.andExpect(jsonPath("$[0].planCode").value("BASIC"))
			.andExpect(jsonPath("$[0].planDisplayName").value("Basic"))
			.andExpect(jsonPath("$[0].name").value("Basic 30 Days"))
			.andExpect(jsonPath("$[0].description").value("Basic description"))
			.andExpect(jsonPath("$[0].priceAmount").isNumber())
			.andExpect(jsonPath("$[0].priceAmount").value(500000))
			.andExpect(jsonPath("$[0].currencyCode").value("VND"))
			.andExpect(jsonPath("$[0].durationDays").value(30))
			.andExpect(jsonPath("$[0].supportsBooking").value(false))
			.andExpect(jsonPath("$[0].supportsPersonalCoaching").value(false))
			.andExpect(jsonPath("$[1].offerId").value(plusOfferId.toString()))
			.andExpect(jsonPath("$[2]").doesNotExist())
			.andReturn();

		Map<String, Object> firstOffer = JsonPath.read(result.getResponse().getContentAsString(), "$[0]");
		assertThat(firstOffer.keySet()).containsExactlyInAnyOrder("offerId", "planCode", "planDisplayName", "name",
			"description", "priceAmount", "currencyCode", "durationDays", "supportsBooking",
			"supportsPersonalCoaching");
		assertThat(countOffers()).isEqualTo(offerCount);
		assertThat(auditEventRepository.count()).isEqualTo(auditCount);
		assertThat(notificationRepository.count()).isEqualTo(notificationCount);
	}

	@Test
	void activeReceptionistCanListOffers() throws Exception {
		Account receptionist = createAccount(AccountRole.RECEPTIONIST, AccountStatus.ACTIVE);
		insertOffer(UUID.randomUUID(), "PLUS", "Reception Offer", "Available", 750000, 45, "ACTIVE",
			receptionist);

		getOffers(accessToken(receptionist), null)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].name").value("Reception Offer"));
	}

	@ParameterizedTest
	@EnumSource(value = AccountRole.class, names = {"COACH", "MANAGER"})
	void coachAndManagerAreForbidden(AccountRole role) throws Exception {
		Account account = createAccount(role, AccountStatus.ACTIVE);

		getOffers(accessToken(account), null).andExpect(status().isForbidden());
	}

	@Test
	void missingAndInvalidBearerTokensAreUnauthorized() throws Exception {
		mockMvc.perform(get(OFFER_PATH).contextPath("/api/v1"))
			.andExpect(status().isUnauthorized());
		getOffers("not-a-jwt", null).andExpect(status().isUnauthorized());
	}

	@Test
	void expiredBearerTokenIsUnauthorized() throws Exception {
		Account member = createAccount(AccountRole.MEMBER, AccountStatus.ACTIVE);
		Instant now = Instant.now();
		JwtClaimsSet claims = JwtClaimsSet.builder()
			.subject(member.getId().toString())
			.issuedAt(now.minusSeconds(1200))
			.expiresAt(now.minusSeconds(300))
			.claim("role", member.getRole().name())
			.build();
		String expiredToken = jwtEncoder.encode(JwtEncoderParameters.from(
			JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();

		getOffers(expiredToken, null).andExpect(status().isUnauthorized());
	}

	@Test
	void tokenIsRejectedAfterAccountStopsBeingActive() throws Exception {
		Account member = createAccount(AccountRole.MEMBER, AccountStatus.ACTIVE);
		String token = accessToken(member);
		jdbcTemplate.update("update accounts set status = 'SUSPENDED' where id = ?", member.getId());
		entityManager.clear();

		getOffers(token, null).andExpect(status().isUnauthorized());
	}

	@ParameterizedTest
	@EnumSource(MembershipPlanCode.class)
	void validPlanFilterReturnsOnlyMatchingActiveOffers(MembershipPlanCode planCode) throws Exception {
		Account member = createAccount(AccountRole.MEMBER, AccountStatus.ACTIVE);
		insertOffer(UUID.randomUUID(), "BASIC", "Basic Active", "Basic", 500000, 30, "ACTIVE", member);
		insertOffer(UUID.randomUUID(), "PLUS", "Plus Active", "Plus", 800000, 30, "ACTIVE", member);
		insertOffer(UUID.randomUUID(), planCode.name(), "Matching Inactive", "Inactive", 100000, 10, "INACTIVE",
			member);

		getOffers(accessToken(member), planCode.name())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(1))
			.andExpect(jsonPath("$[0].planCode").value(planCode.name()));
	}

	@ParameterizedTest
	@ValueSource(strings = {"basic", "plus", " BASIC ", "PREMIUM", ""})
	void invalidPlanCodeReturnsProblemDetail(String planCode) throws Exception {
		Account member = createAccount(AccountRole.MEMBER, AccountStatus.ACTIVE);

		getOffers(accessToken(member), planCode)
			.andExpect(status().isBadRequest())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.code").value("INVALID_PLAN_CODE"))
			.andExpect(jsonPath("$.status").value(400));
	}

	@Test
	void noMatchingOfferReturnsAnEmptyJsonArray() throws Exception {
		Account member = createAccount(AccountRole.MEMBER, AccountStatus.ACTIVE);
		insertOffer(UUID.randomUUID(), "BASIC", "Basic Only", "Basic", 500000, 30, "ACTIVE", member);

		getOffers(accessToken(member), "PLUS")
			.andExpect(status().isOk())
			.andExpect(content().json("[]"));
	}

	@Test
	void offersUseStablePlanDurationNameAndIdOrdering() throws Exception {
		Account member = createAccount(AccountRole.MEMBER, AccountStatus.ACTIVE);
		UUID basicAlphaFirst = UUID.fromString("00000000-0000-0000-0000-000000000001");
		UUID basicAlphaSecond = UUID.fromString("00000000-0000-0000-0000-000000000002");
		UUID basicBeta = UUID.fromString("00000000-0000-0000-0000-000000000003");
		UUID basicLong = UUID.fromString("00000000-0000-0000-0000-000000000004");
		UUID plusShort = UUID.fromString("00000000-0000-0000-0000-000000000005");
		insertOffer(basicLong, "BASIC", "Long", "Long", 900000, 90, "ACTIVE", member);
		insertOffer(basicBeta, "BASIC", "Beta", "Beta", 500000, 30, "ACTIVE", member);
		insertOffer(plusShort, "PLUS", "Plus", "Plus", 400000, 10, "ACTIVE", member);
		insertOffer(basicAlphaSecond, "BASIC", "Alpha", "Alpha second", 500000, 30, "ACTIVE", member);
		insertOffer(basicAlphaFirst, "BASIC", "Alpha", "Alpha first", 500000, 30, "ACTIVE", member);

		String body = getOffers(accessToken(member), null).andExpect(status().isOk())
			.andReturn().getResponse().getContentAsString();
		List<String> ids = JsonPath.read(body, "$[*].offerId");

		assertThat(ids).containsExactly(basicAlphaFirst.toString(), basicAlphaSecond.toString(),
			basicBeta.toString(), basicLong.toString(), plusShort.toString());
	}

	@Test
	void planDisplayAndCapabilitiesComeFromMembershipPlansTable() throws Exception {
		Account member = createAccount(AccountRole.MEMBER, AccountStatus.ACTIVE);
		jdbcTemplate.update("""
			update membership_plans
			set display_name = 'Plus from database', supports_booking = false,
			    supports_personal_coaching = false
			where plan_code = 'PLUS'
			""");
		insertOffer(UUID.randomUUID(), "PLUS", "Database Plan Offer", "Plan source proof", 800000, 30, "ACTIVE",
			member);

		getOffers(accessToken(member), "PLUS")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].planDisplayName").value("Plus from database"))
			.andExpect(jsonPath("$[0].supportsBooking").value(false))
			.andExpect(jsonPath("$[0].supportsPersonalCoaching").value(false));
	}

	private Account createAccount(AccountRole role, AccountStatus status) {
		UUID id = UUID.randomUUID();
		return accountRepository.saveAndFlush(new Account(id, role, status, role.name() + " Offer Test",
			"09" + id.toString().replace("-", "").substring(0, 8), id + "@example.test",
			LocalDate.of(1990, 1, 1), passwordEncoder.encode(PASSWORD)));
	}

	private void insertOffer(UUID id, String planCode, String name, String description, long priceAmount,
			int durationDays, String status, Account creator) {
		jdbcTemplate.update("""
			insert into membership_offers
			    (id, plan_code, name, description, price_amount, currency_code, duration_days, status,
			     created_by_account_id)
			values (?, ?, ?, ?, ?, 'VND', ?, ?, ?)
			""", id, planCode, name, description, priceAmount, durationDays, status, creator.getId());
	}

	private long countOffers() {
		return jdbcTemplate.queryForObject("select count(*) from membership_offers", Long.class);
	}

	private String accessToken(Account account) throws Exception {
		MvcResult result = mockMvc.perform(post(LOGIN_PATH)
			.contextPath("/api/v1")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"identifier\":\"%s\",\"password\":\"%s\"}".formatted(account.getEmail(), PASSWORD)))
			.andExpect(status().isOk())
			.andReturn();
		return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
	}

	private ResultActions getOffers(String accessToken, String planCode) throws Exception {
		var request = get(OFFER_PATH)
			.contextPath("/api/v1")
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
		if (planCode != null) {
			request.queryParam("planCode", planCode);
		}
		return mockMvc.perform(request);
	}
}
