package com.scms.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
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
import com.scms.backend.audit.AuditEventRepository;
import com.scms.backend.notification.NotificationRepository;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.Cookie;

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
class ChangePasswordIntegrationTests {

	private static final String CURRENT_PASSWORD = "correct horse battery staple";
	private static final String NEW_PASSWORD = "new correct horse battery staple";
	private static final String LOGIN_PATH = "/api/v1/auth/login";
	private static final String REFRESH_PATH = "/api/v1/auth/refresh";
	private static final String CHANGE_PASSWORD_PATH = "/api/v1/auth/change-password";

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

	@ParameterizedTest
	@EnumSource(AccountRole.class)
	void everyActiveRoleCanChangeItsOwnPassword(AccountRole role) throws Exception {
		Account account = createAccount(role, AccountStatus.ACTIVE);
		MvcResult login = performLogin(account.getEmail(), CURRENT_PASSWORD).andExpect(status().isOk()).andReturn();

		performChange(requireAccessToken(login), CURRENT_PASSWORD, NEW_PASSWORD, requireRefreshCookie(login))
			.andExpect(status().isNoContent());

		Account changed = reload(account.getId());
		assertThat(passwordEncoder.matches(NEW_PASSWORD, changed.getPasswordHash())).isTrue();
	}

	@Test
	void successfulChangeReturnsEmpty204UpdatesBcryptRevokesSessionAndCreatesNoSideEffects() throws Exception {
		Account account = createAccount(AccountRole.MEMBER, AccountStatus.ACTIVE);
		String oldHash = account.getPasswordHash();
		long auditCount = auditEventRepository.count();
		long notificationCount = notificationRepository.count();
		MvcResult login = performLogin(account.getEmail(), CURRENT_PASSWORD).andExpect(status().isOk()).andReturn();
		Cookie refreshCookie = requireRefreshCookie(login);

		MvcResult changedResult = performChange(requireAccessToken(login), CURRENT_PASSWORD, NEW_PASSWORD, refreshCookie)
			.andExpect(status().isNoContent())
			.andExpect(content().string(""))
			.andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("refresh_token=")))
			.andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Path=/api/v1/auth")))
			.andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")))
			.andReturn();

		Cookie clearedCookie = changedResult.getResponse().getCookie(RefreshTokenCookieService.COOKIE_NAME);
		assertThat(clearedCookie).isNotNull();
		assertThat(clearedCookie.getValue()).isEmpty();
		assertThat(clearedCookie.getMaxAge()).isZero();
		assertThat(clearedCookie.getPath()).isEqualTo(RefreshTokenCookieService.COOKIE_PATH);

		Account changed = reload(account.getId());
		assertThat(changed.getPasswordHash()).startsWith("$2").isNotEqualTo(oldHash);
		assertThat(passwordEncoder.matches(NEW_PASSWORD, changed.getPasswordHash())).isTrue();
		assertThat(refreshTokenRepository.findAllByAccountId(account.getId()))
			.singleElement()
			.extracting(RefreshToken::getRevokedAt)
			.isNotNull();
		assertThat(auditEventRepository.count()).isEqualTo(auditCount);
		assertThat(notificationRepository.count()).isEqualTo(notificationCount);

		performRefresh(refreshCookie)
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
		performLogin(account.getEmail(), CURRENT_PASSWORD).andExpect(status().isUnauthorized());
		performLogin(account.getEmail(), NEW_PASSWORD).andExpect(status().isOk());
	}

	@Test
	void jwtSubjectAloneSelectsTheAccountToChange() throws Exception {
		Account authenticated = createAccount(AccountRole.COACH, AccountStatus.ACTIVE);
		Account other = createAccount(AccountRole.COACH, AccountStatus.ACTIVE);
		String otherHash = other.getPasswordHash();
		MvcResult login = performLogin(authenticated.getEmail(), CURRENT_PASSWORD)
			.andExpect(status().isOk()).andReturn();

		performChange(requireAccessToken(login), CURRENT_PASSWORD, NEW_PASSWORD, requireRefreshCookie(login))
			.andExpect(status().isNoContent());

		assertThat(passwordEncoder.matches(NEW_PASSWORD, reload(authenticated.getId()).getPasswordHash())).isTrue();
		assertThat(reload(other.getId()).getPasswordHash()).isEqualTo(otherHash);
	}

	@Test
	void incorrectCurrentPasswordChangesNothingAndKeepsRefreshTokenUsable() throws Exception {
		Account account = createAccount(AccountRole.RECEPTIONIST, AccountStatus.ACTIVE);
		String oldHash = account.getPasswordHash();
		MvcResult login = performLogin(account.getEmail(), CURRENT_PASSWORD).andExpect(status().isOk()).andReturn();
		Cookie refreshCookie = requireRefreshCookie(login);

		performChange(requireAccessToken(login), "incorrect current password", NEW_PASSWORD, refreshCookie)
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("CURRENT_PASSWORD_INCORRECT"))
			.andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));

		assertThat(reload(account.getId()).getPasswordHash()).isEqualTo(oldHash);
		assertThat(refreshTokenRepository.findAllByAccountId(account.getId()))
			.singleElement().extracting(RefreshToken::getRevokedAt).isNull();
		performRefresh(refreshCookie).andExpect(status().isOk());
	}

	@Test
	void sameNewPasswordIsDetectedWithBcryptAndKeepsRefreshTokenUsable() throws Exception {
		Account account = createAccount(AccountRole.MANAGER, AccountStatus.ACTIVE);
		String oldHash = account.getPasswordHash();
		MvcResult login = performLogin(account.getPhone(), CURRENT_PASSWORD).andExpect(status().isOk()).andReturn();
		Cookie refreshCookie = requireRefreshCookie(login);

		performChange(requireAccessToken(login), CURRENT_PASSWORD, CURRENT_PASSWORD, refreshCookie)
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("NEW_PASSWORD_SAME_AS_CURRENT"))
			.andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));

		assertThat(reload(account.getId()).getPasswordHash()).isEqualTo(oldHash);
		assertThat(refreshTokenRepository.findAllByAccountId(account.getId()))
			.singleElement().extracting(RefreshToken::getRevokedAt).isNull();
		performRefresh(refreshCookie).andExpect(status().isOk());
	}

	@Test
	void shortNewPasswordReturnsValidationErrorWithoutMutation() throws Exception {
		Account account = createAccount(AccountRole.MEMBER, AccountStatus.ACTIVE);
		String oldHash = account.getPasswordHash();
		MvcResult login = performLogin(account.getEmail(), CURRENT_PASSWORD).andExpect(status().isOk()).andReturn();

		performChange(requireAccessToken(login), CURRENT_PASSWORD, "short", requireRefreshCookie(login))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
			.andExpect(jsonPath("$.errors.newPassword").exists())
			.andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));

		assertThat(reload(account.getId()).getPasswordHash()).isEqualTo(oldHash);
		assertThat(refreshTokenRepository.findAllByAccountId(account.getId()))
			.singleElement().extracting(RefreshToken::getRevokedAt).isNull();
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"{}",
		"{\"currentPassword\":null,\"newPassword\":\"valid password\"}",
		"{\"currentPassword\":\"\",\"newPassword\":\"valid password\"}",
		"{\"currentPassword\":\"valid password\",\"newPassword\":null}",
		"{\"currentPassword\":\"valid password\",\"newPassword\":\"   \"}"
	})
	void nullBlankOrMissingFieldsReturnValidationError(String requestBody) throws Exception {
		Account account = createAccount(AccountRole.MEMBER, AccountStatus.ACTIVE);
		MvcResult login = performLogin(account.getEmail(), CURRENT_PASSWORD).andExpect(status().isOk()).andReturn();

		performChange(requireAccessToken(login), requestBody, requireRefreshCookie(login))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
			.andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
	}

	@ParameterizedTest
	@ValueSource(strings = {"confirmPassword", "accountId", "password", "unexpected"})
	void unknownOrForbiddenFieldsReturnMalformedRequest(String fieldName) throws Exception {
		Account account = createAccount(AccountRole.MEMBER, AccountStatus.ACTIVE);
		MvcResult login = performLogin(account.getEmail(), CURRENT_PASSWORD).andExpect(status().isOk()).andReturn();
		String body = "{\"currentPassword\":\"%s\",\"newPassword\":\"%s\",\"%s\":\"forbidden\"}"
			.formatted(CURRENT_PASSWORD, NEW_PASSWORD, fieldName);

		performChange(requireAccessToken(login), body, requireRefreshCookie(login))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"))
			.andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
	}

	@Test
	void malformedJsonReturnsStableErrorWithoutLeakingPasswordValues() throws Exception {
		Account account = createAccount(AccountRole.MEMBER, AccountStatus.ACTIVE);
		MvcResult login = performLogin(account.getEmail(), CURRENT_PASSWORD).andExpect(status().isOk()).andReturn();

		String response = performChange(requireAccessToken(login),
			"{\"currentPassword\":\"raw-current-secret\",\"newPassword\":", requireRefreshCookie(login))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"))
			.andReturn().getResponse().getContentAsString();

		assertThat(response).doesNotContain("raw-current-secret", account.getPasswordHash());
	}

	@Test
	void missingOrInvalidBearerTokenIsUnauthorized() throws Exception {
		String body = changeBody(CURRENT_PASSWORD, NEW_PASSWORD);
		mockMvc.perform(post(CHANGE_PASSWORD_PATH).contextPath("/api/v1")
			.contentType(MediaType.APPLICATION_JSON).content(body))
			.andExpect(status().isUnauthorized());
		mockMvc.perform(post(CHANGE_PASSWORD_PATH).contextPath("/api/v1")
			.header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt")
			.contentType(MediaType.APPLICATION_JSON).content(body))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void expiredBearerTokenIsUnauthorized() throws Exception {
		Account account = createAccount(AccountRole.MEMBER, AccountStatus.ACTIVE);
		Instant now = Instant.now();
		JwtClaimsSet claims = JwtClaimsSet.builder()
			.subject(account.getId().toString())
			.issuedAt(now.minusSeconds(1200))
			.expiresAt(now.minusSeconds(300))
			.claim("role", account.getRole().name())
			.build();
		String expiredToken = jwtEncoder.encode(JwtEncoderParameters.from(
			JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();

		performChange(expiredToken, changeBody(CURRENT_PASSWORD, NEW_PASSWORD), null)
			.andExpect(status().isUnauthorized());
	}

	@ParameterizedTest
	@EnumSource(value = AccountStatus.class, names = {"SUSPENDED", "INACTIVE"})
	void tokenIsRejectedAfterAccountStopsBeingActive(AccountStatus status) throws Exception {
		AccountRole role = status == AccountStatus.SUSPENDED ? AccountRole.MEMBER : AccountRole.COACH;
		Account account = createAccount(role, AccountStatus.ACTIVE);
		MvcResult login = performLogin(account.getEmail(), CURRENT_PASSWORD).andExpect(status().isOk()).andReturn();
		String accessToken = requireAccessToken(login);
		jdbcTemplate.update("update accounts set status = ? where id = ?", status.name(), account.getId());
		entityManager.clear();

		performChange(accessToken, changeBody(CURRENT_PASSWORD, NEW_PASSWORD), requireRefreshCookie(login))
			.andExpect(status().isUnauthorized());
	}

	@ParameterizedTest
	@ValueSource(strings = {"missing", "invalid"})
	void absentOrInvalidRefreshCookieStillChangesPasswordAndClearsCookie(String cookieCase) throws Exception {
		Account account = createAccount(AccountRole.COACH, AccountStatus.ACTIVE);
		MvcResult login = performLogin(account.getEmail(), CURRENT_PASSWORD).andExpect(status().isOk()).andReturn();
		Cookie cookie = "invalid".equals(cookieCase) ? new Cookie(RefreshTokenCookieService.COOKIE_NAME, "invalid-token") : null;

		performChange(requireAccessToken(login), changeBody(CURRENT_PASSWORD, NEW_PASSWORD), cookie)
			.andExpect(status().isNoContent())
			.andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));

		assertThat(passwordEncoder.matches(NEW_PASSWORD, reload(account.getId()).getPasswordHash())).isTrue();
	}

	@Test
	void onlyRefreshTokenPresentedByTheCurrentSessionIsRevoked() throws Exception {
		Account account = createAccount(AccountRole.MANAGER, AccountStatus.ACTIVE);
		MvcResult firstLogin = performLogin(account.getEmail(), CURRENT_PASSWORD).andExpect(status().isOk()).andReturn();
		MvcResult secondLogin = performLogin(account.getEmail(), CURRENT_PASSWORD).andExpect(status().isOk()).andReturn();
		Cookie firstCookie = requireRefreshCookie(firstLogin);
		Cookie secondCookie = requireRefreshCookie(secondLogin);

		performChange(requireAccessToken(firstLogin), CURRENT_PASSWORD, NEW_PASSWORD, firstCookie)
			.andExpect(status().isNoContent());

		List<RefreshToken> tokens = refreshTokenRepository.findAllByAccountId(account.getId());
		assertThat(tokens).hasSize(2);
		assertThat(tokens).filteredOn(token -> token.getRevokedAt() != null).hasSize(1);
		assertThat(tokens).filteredOn(token -> token.getRevokedAt() == null).hasSize(1);
		performRefresh(firstCookie).andExpect(status().isUnauthorized());
		performRefresh(secondCookie).andExpect(status().isOk());
	}

	@Test
	void requestStringRepresentationRedactsBothPasswords() {
		String representation = new ChangePasswordRequest("raw-current-secret", "raw-new-secret").toString();
		String loginRepresentation = new LoginRequest("member@example.test", "raw-login-secret").toString();

		assertThat(representation)
			.doesNotContain("raw-current-secret", "raw-new-secret")
			.contains("currentPassword=[REDACTED]", "newPassword=[REDACTED]");
		assertThat(loginRepresentation)
			.doesNotContain("raw-login-secret")
			.contains("password=[REDACTED]");
	}

	private Account createAccount(AccountRole role, AccountStatus status) {
		UUID id = UUID.randomUUID();
		return accountRepository.saveAndFlush(new Account(id, role, status, "Change Password Test",
			"09" + id.toString().replace("-", "").substring(0, 8), id + "@example.test",
			LocalDate.of(1990, 1, 1), passwordEncoder.encode(CURRENT_PASSWORD)));
	}

	private Account reload(UUID accountId) {
		entityManager.flush();
		entityManager.clear();
		return accountRepository.findById(accountId).orElseThrow();
	}

	private ResultActions performLogin(String identifier, String password) throws Exception {
		return mockMvc.perform(post(LOGIN_PATH)
			.contextPath("/api/v1")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"identifier\":\"%s\",\"password\":\"%s\"}".formatted(identifier, password)));
	}

	private ResultActions performRefresh(Cookie cookie) throws Exception {
		return mockMvc.perform(post(REFRESH_PATH).contextPath("/api/v1").cookie(cookie));
	}

	private ResultActions performChange(String accessToken, String currentPassword, String newPassword, Cookie cookie)
			throws Exception {
		return performChange(accessToken, changeBody(currentPassword, newPassword), cookie);
	}

	private ResultActions performChange(String accessToken, String body, Cookie cookie) throws Exception {
		var request = post(CHANGE_PASSWORD_PATH)
			.contextPath("/api/v1")
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
			.contentType(MediaType.APPLICATION_JSON)
			.content(body);
		if (cookie != null) {
			request.cookie(cookie);
		}
		return mockMvc.perform(request);
	}

	private String changeBody(String currentPassword, String newPassword) {
		return "{\"currentPassword\":\"%s\",\"newPassword\":\"%s\"}"
			.formatted(currentPassword, newPassword);
	}

	private Cookie requireRefreshCookie(MvcResult result) {
		Cookie cookie = result.getResponse().getCookie(RefreshTokenCookieService.COOKIE_NAME);
		assertThat(cookie).isNotNull();
		return cookie;
	}

	private String requireAccessToken(MvcResult result) throws Exception {
		return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
	}
}
