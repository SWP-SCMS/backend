package com.scms.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;

import com.jayway.jsonpath.JsonPath;
import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
@Import(AuthenticationIntegrationTests.ProtectedTestController.class)
class AuthenticationIntegrationTests {

	private static final String PASSWORD = "correct horse battery staple";
	private static final String LOGIN_PATH = "/api/v1/auth/login";
	private static final String REFRESH_PATH = "/api/v1/auth/refresh";
	private static final String LOGOUT_PATH = "/api/v1/auth/logout";

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
	private RefreshTokenRepository refreshTokenRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private JwtDecoder jwtDecoder;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private EntityManager entityManager;

	@Test
	void loginByEmailNormalizesIdentifierAndReturnsJwtRoleAndLocalCookie() throws Exception {
		Account account = createAccount(AccountRole.MANAGER, AccountStatus.ACTIVE);

		MvcResult result = performLogin("  " + account.getEmail().toUpperCase() + "  ", PASSWORD)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.accountId").value(account.getId().toString()))
			.andExpect(jsonPath("$.fullName").value(account.getFullName()))
			.andExpect(jsonPath("$.role").value("MANAGER"))
			.andExpect(jsonPath("$.tokenType").value("Bearer"))
			.andExpect(jsonPath("$.expiresIn").value(900))
			.andExpect(jsonPath("$.refreshToken").doesNotExist())
			.andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")))
			.andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Path=/api/v1/auth")))
			.andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("SameSite=Lax")))
			.andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=604800")))
			.andExpect(header().string(HttpHeaders.SET_COOKIE, not(containsString("Secure"))))
			.andReturn();

		String accessToken = JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
		Jwt jwt = jwtDecoder.decode(accessToken);
		assertThat(jwt.getSubject()).isEqualTo(account.getId().toString());
		assertThat(jwt.getClaimAsString("role")).isEqualTo("MANAGER");
		assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt())).isEqualTo(Duration.ofMinutes(15));

		Cookie refreshCookie = requireRefreshCookie(result);
		List<RefreshToken> persistedTokens = refreshTokenRepository.findAllByAccountId(account.getId());
		assertThat(persistedTokens).hasSize(1);
		assertThat(persistedTokens.getFirst().getTokenHash())
			.matches("[0-9a-f]{64}")
			.isNotEqualTo(refreshCookie.getValue());
	}

	@Test
	void loginByPhoneTrimsIdentifierAndReturnsSession() throws Exception {
		Account account = createAccount(AccountRole.COACH, AccountStatus.ACTIVE);

		performLogin("  " + account.getPhone() + "  ", PASSWORD)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.accountId").value(account.getId().toString()))
			.andExpect(jsonPath("$.role").value("COACH"))
			.andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")));
	}

	@Test
	void loginRejectsUnknownIdentifierWrongPasswordAndNonActiveAccountsWithoutDisclosure() throws Exception {
		Account active = createAccount(AccountRole.COACH, AccountStatus.ACTIVE);
		Account suspended = createAccount(AccountRole.MEMBER, AccountStatus.SUSPENDED);
		Account inactive = createAccount(AccountRole.RECEPTIONIST, AccountStatus.INACTIVE);

		String unknown = performLogin("missing@example.test", PASSWORD)
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
			.andExpect(jsonPath("$.detail").value("Invalid credentials"))
			.andReturn().getResponse().getContentAsString();
		String wrongPassword = performLogin(active.getEmail(), "wrong-password")
			.andExpect(status().isUnauthorized())
			.andReturn().getResponse().getContentAsString();
		String suspendedResponse = performLogin(suspended.getEmail(), PASSWORD)
			.andExpect(status().isUnauthorized())
			.andReturn().getResponse().getContentAsString();
		String inactiveResponse = performLogin(inactive.getEmail(), PASSWORD)
			.andExpect(status().isUnauthorized())
			.andReturn().getResponse().getContentAsString();

		assertThat(wrongPassword).isEqualTo(unknown);
		assertThat(suspendedResponse).isEqualTo(unknown);
		assertThat(inactiveResponse).isEqualTo(unknown);
	}

	@Test
	void loginUsesTheExistingValidationEnvelope() throws Exception {
		mockMvc.perform(post(LOGIN_PATH)
			.contextPath("/api/v1")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"identifier\":\"\",\"password\":\"\"}"))
			.andExpect(status().isBadRequest())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
			.andExpect(jsonPath("$.errors.identifier").exists())
			.andExpect(jsonPath("$.errors.password").exists());
	}

	@Test
	void refreshRotatesTokenAndOldTokenCannotBeReused() throws Exception {
		Account account = createAccount(AccountRole.COACH, AccountStatus.ACTIVE);
		Cookie originalCookie = requireRefreshCookie(performLogin(account.getEmail(), PASSWORD)
			.andExpect(status().isOk()).andReturn());

		MvcResult refreshResult = performRefresh(originalCookie)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.role").value("COACH"))
			.andReturn();
		Cookie rotatedCookie = requireRefreshCookie(refreshResult);
		assertThat(rotatedCookie.getValue()).isNotEqualTo(originalCookie.getValue());

		List<RefreshToken> tokens = refreshTokenRepository.findAllByAccountId(account.getId());
		assertThat(tokens).hasSize(2);
		RefreshToken revoked = tokens.stream().filter(token -> token.getRevokedAt() != null).findFirst().orElseThrow();
		RefreshToken active = tokens.stream().filter(token -> token.getRevokedAt() == null).findFirst().orElseThrow();
		assertThat(revoked.getReplacedByTokenId()).isEqualTo(active.getId());

		performRefresh(originalCookie)
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
		performRefresh(rotatedCookie).andExpect(status().isOk());
	}

	@Test
	void refreshRejectsExpiredRevokedAndNowInactiveSessions() throws Exception {
		Account expiredAccount = createAccount(AccountRole.COACH, AccountStatus.ACTIVE);
		Cookie expiredCookie = requireRefreshCookie(performLogin(expiredAccount.getEmail(), PASSWORD)
			.andExpect(status().isOk()).andReturn());
		jdbcTemplate.update("""
			update refresh_tokens
			set created_at = current_timestamp - interval '8 days',
			    expires_at = current_timestamp - interval '1 day'
			where account_id = ?
			""", expiredAccount.getId());
		entityManager.clear();
		performRefresh(expiredCookie).andExpect(status().isUnauthorized());

		Account revokedAccount = createAccount(AccountRole.MANAGER, AccountStatus.ACTIVE);
		Cookie revokedCookie = requireRefreshCookie(performLogin(revokedAccount.getEmail(), PASSWORD)
			.andExpect(status().isOk()).andReturn());
		jdbcTemplate.update("update refresh_tokens set revoked_at = current_timestamp where account_id = ?",
			revokedAccount.getId());
		entityManager.clear();
		performRefresh(revokedCookie).andExpect(status().isUnauthorized());

		Account inactiveAccount = createAccount(AccountRole.RECEPTIONIST, AccountStatus.ACTIVE);
		Cookie inactiveCookie = requireRefreshCookie(performLogin(inactiveAccount.getEmail(), PASSWORD)
			.andExpect(status().isOk()).andReturn());
		jdbcTemplate.update("update accounts set status = 'INACTIVE' where id = ?", inactiveAccount.getId());
		entityManager.clear();
		performRefresh(inactiveCookie).andExpect(status().isUnauthorized());
	}

	@Test
	void logoutRevokesCurrentTokenClearsCookieAndIsIdempotent() throws Exception {
		Account account = createAccount(AccountRole.COACH, AccountStatus.ACTIVE);
		Cookie refreshCookie = requireRefreshCookie(performLogin(account.getEmail(), PASSWORD)
			.andExpect(status().isOk()).andReturn());

		performLogout(refreshCookie)
			.andExpect(status().isNoContent())
			.andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("refresh_token=")))
			.andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")))
			.andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Path=/api/v1/auth")))
			.andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")))
			.andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("SameSite=Lax")));
		performLogout(refreshCookie).andExpect(status().isNoContent());
		performRefresh(refreshCookie).andExpect(status().isUnauthorized());

		assertThat(refreshTokenRepository.findAllByAccountId(account.getId()))
			.singleElement()
			.extracting(RefreshToken::getRevokedAt)
			.isNotNull();
	}

	@Test
	void protectedRequestsRejectMissingAndInvalidBearerTokens() throws Exception {
		mockMvc.perform(get("/api/v1/protected-test").contextPath("/api/v1"))
			.andExpect(status().isUnauthorized());
		mockMvc.perform(get("/api/v1/protected-test")
			.contextPath("/api/v1")
			.header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void activeAccessTokenIsRejectedAfterAccountBecomesSuspendedOrInactive() throws Exception {
		Account member = createAccount(AccountRole.MEMBER, AccountStatus.ACTIVE);
		String memberAccessToken = requireAccessToken(performLogin(member.getEmail(), PASSWORD)
			.andExpect(status().isOk()).andReturn());
		performProtectedRequest(memberAccessToken).andExpect(status().isOk());

		jdbcTemplate.update("update accounts set status = 'SUSPENDED' where id = ?", member.getId());
		entityManager.clear();
		performProtectedRequest(memberAccessToken).andExpect(status().isUnauthorized());

		Account coach = createAccount(AccountRole.COACH, AccountStatus.ACTIVE);
		String coachAccessToken = requireAccessToken(performLogin(coach.getPhone(), PASSWORD)
			.andExpect(status().isOk()).andReturn());
		performProtectedRequest(coachAccessToken).andExpect(status().isOk());

		jdbcTemplate.update("update accounts set status = 'INACTIVE' where id = ?", coach.getId());
		entityManager.clear();
		performProtectedRequest(coachAccessToken).andExpect(status().isUnauthorized());
	}

	@Test
	void credentialedCorsPreflightWorksForLogin() throws Exception {
		mockMvc.perform(options(LOGIN_PATH)
			.contextPath("/api/v1")
			.header(HttpHeaders.ORIGIN, "http://localhost:5173")
			.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
			.andExpect(status().isOk())
			.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"))
			.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
	}

	private Account createAccount(AccountRole role, AccountStatus status) {
		UUID id = UUID.randomUUID();
		return accountRepository.saveAndFlush(new Account(id, role, status, "Authentication Test",
			"09" + id.toString().replace("-", "").substring(0, 9), id + "@example.test",
			LocalDate.of(1990, 1, 1), passwordEncoder.encode(PASSWORD)));
	}

	private org.springframework.test.web.servlet.ResultActions performLogin(String identifier, String password)
			throws Exception {
		return mockMvc.perform(post(LOGIN_PATH)
			.contextPath("/api/v1")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"identifier\":\"%s\",\"password\":\"%s\"}".formatted(identifier, password)));
	}

	private org.springframework.test.web.servlet.ResultActions performProtectedRequest(String accessToken)
			throws Exception {
		return mockMvc.perform(get("/api/v1/protected-test")
			.contextPath("/api/v1")
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken));
	}

	private org.springframework.test.web.servlet.ResultActions performRefresh(Cookie cookie) throws Exception {
		return mockMvc.perform(post(REFRESH_PATH)
			.contextPath("/api/v1")
			.cookie(cookie));
	}

	private org.springframework.test.web.servlet.ResultActions performLogout(Cookie cookie) throws Exception {
		return mockMvc.perform(post(LOGOUT_PATH)
			.contextPath("/api/v1")
			.cookie(cookie));
	}

	private Cookie requireRefreshCookie(MvcResult result) {
		Cookie cookie = result.getResponse().getCookie(RefreshTokenCookieService.COOKIE_NAME);
		assertThat(cookie).isNotNull();
		return cookie;
	}

	private String requireAccessToken(MvcResult result) throws Exception {
		return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
	}

	@RestController
	static class ProtectedTestController {

		@GetMapping("/protected-test")
		String protectedTest() {
			return "ok";
		}
	}
}
