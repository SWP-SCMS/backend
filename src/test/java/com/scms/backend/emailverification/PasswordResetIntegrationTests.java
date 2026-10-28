package com.scms.backend.emailverification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.TimeZone;
import java.util.UUID;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
	"scms.email-verification.enabled=true",
	"scms.email-verification.hmac-active-version=v1",
	"scms.email-verification.hmac-keys=v1:MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=",
	"scms.email-verification.encryption-active-version=v1",
	"scms.email-verification.encryption-keys=v1:YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXpBQkNERUY=",
	"scms.email-verification.rate-limit-key=cmF0ZS1saW1pdC1rZXktMzItYnl0ZXMtbG9uZyEhISE=",
	"scms.email-verification.cookie-secure=false",
	"spring.mail.host=localhost",
	"scms.email.smtp-username=no-reply@example.test",
	"scms.email.from=no-reply@example.test",
	"scms.email-verification.dispatch-delay=3600000",
	"scms.email-verification.dispatch-initial-delay=3600000"
})
@Transactional
class PasswordResetIntegrationTests {
	static { TimeZone.setDefault(TimeZone.getTimeZone(ZoneOffset.UTC)); }
	@Container @ServiceConnection
	static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

	@Autowired MockMvc mvc;
	@Autowired JdbcTemplate db;
	@Autowired AccountRepository accounts;
	@Autowired PasswordEncoder passwords;
	@Autowired EmailVerificationCrypto crypto;

	@Test
	void resetUsesOneTimeOtpSessionRevokesRefreshTokensAndDoesNotLogin() throws Exception {
		UUID accountId = createAccount("reset@example.test");
		MvcResult login = mvc.perform(post("/api/v1/auth/login").contextPath("/api/v1")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"identifier\":\"reset@example.test\",\"password\":\"OldPassword123\"}"))
			.andExpect(status().isOk()).andReturn();
		assertThat(login.getResponse().getCookie("refresh_token")).isNotNull();

		mvc.perform(post("/api/v1/auth/password-reset/request").contextPath("/api/v1")
			.contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"RESET@example.test\"}"))
			.andExpect(status().isAccepted()).andExpect(jsonPath("$.accepted").value(true));
		UUID requestId = db.queryForObject("select id from password_reset_requests where account_id=?", UUID.class, accountId);
		Integer generation = db.queryForObject("select generation from password_reset_requests where id=?", Integer.class, requestId);
		db.update("update password_reset_requests set otp_digest=? where id=?",
			crypto.otpDigest("PASSWORD_RESET:" + requestId + ":" + generation + ":012345", "v1"), requestId);
		assertThat(db.queryForObject("select template_key from email_outbox where event_key like 'PASSWORD_RESET_OTP:%'",
			String.class)).isEqualTo("PASSWORD_RESET_OTP");
		assertThat(db.queryForMap("select payload_format,delivery_provider from email_outbox where event_key like 'PASSWORD_RESET_OTP:%'"))
			.containsEntry("payload_format","EMAIL_REQUEST_V1").containsEntry("delivery_provider","smtp");

		MvcResult verified = mvc.perform(post("/api/v1/auth/password-reset/verify").contextPath("/api/v1")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"email\":\"reset@example.test\",\"otp\":\"012345\"}"))
			.andExpect(status().isOk()).andExpect(jsonPath("$.verified").value(true))
			.andExpect(cookie().httpOnly(PasswordResetCookieService.COOKIE_NAME, true)).andReturn();
		Cookie resetSession = verified.getResponse().getCookie(PasswordResetCookieService.COOKIE_NAME);

		mvc.perform(post("/api/v1/auth/password-reset/confirm").contextPath("/api/v1")
			.cookie(resetSession).contentType(MediaType.APPLICATION_JSON)
			.content("{\"newPassword\":\"NewPassword123\",\"confirmPassword\":\"NewPassword123\"}"))
			.andExpect(status().isNoContent())
			.andExpect(cookie().maxAge(PasswordResetCookieService.COOKIE_NAME, 0));

		Account changed = accounts.findById(accountId).orElseThrow();
		assertThat(passwords.matches("NewPassword123", changed.getPasswordHash())).isTrue();
		assertThat(db.queryForObject("select count(*) from refresh_tokens where account_id=? and revoked_at is null",
			Integer.class, accountId)).isZero();
		assertThat(db.queryForObject("select status from password_reset_requests where id=?", String.class, requestId))
			.isEqualTo("COMPLETED");
		assertThat(db.queryForObject("select count(*) from notification_outbox where event_key=?", Integer.class,
			"PASSWORD_RESET_COMPLETED:" + requestId)).isEqualTo(1);
		assertThat(verified.getResponse().getCookie("refresh_token")).isNull();

		mvc.perform(post("/api/v1/auth/password-reset/confirm").contextPath("/api/v1")
			.cookie(resetSession).contentType(MediaType.APPLICATION_JSON)
			.content("{\"newPassword\":\"AnotherPassword123\",\"confirmPassword\":\"AnotherPassword123\"}"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("INVALID_PASSWORD_RESET_SESSION"));
	}

	@Test
	void requestDoesNotRevealWhetherAnAccountExists() throws Exception {
		mvc.perform(post("/api/v1/auth/password-reset/request").contextPath("/api/v1")
			.contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"missing@example.test\"}"))
			.andExpect(status().isAccepted()).andExpect(jsonPath("$.accepted").value(true));
		assertThat(db.queryForObject("select count(*) from password_reset_requests", Integer.class)).isZero();
	}

	@Test
	void failedOtpAttemptsPersistAndCurrentPasswordIsRejected() throws Exception {
		UUID accountId = createAccount("attempts@example.test");
		mvc.perform(post("/api/v1/auth/password-reset/request").contextPath("/api/v1")
			.contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"attempts@example.test\"}"))
			.andExpect(status().isAccepted());
		mvc.perform(post("/api/v1/auth/password-reset/verify").contextPath("/api/v1")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"email\":\"attempts@example.test\",\"otp\":\"999999\"}"))
			.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_OR_EXPIRED_OTP"));
		assertThat(db.queryForObject("select attempt_count from password_reset_requests where account_id=?",
			Integer.class, accountId)).isEqualTo(1);

		mvc.perform(post("/api/v1/auth/password-reset/confirm").contextPath("/api/v1")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"newPassword\":\"NewPassword123\",\"confirmPassword\":\"NewPassword123\",\"currentPassword\":\"OldPassword123\"}"))
			.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
	}

	private UUID createAccount(String email) {
		UUID id = UUID.randomUUID();
		accounts.saveAndFlush(new Account(id, AccountRole.MEMBER, AccountStatus.ACTIVE, "Reset Member",
			"09" + id.toString().replace("-", "").substring(0, 8), email, LocalDate.of(2000, 1, 1),
			passwords.encode("OldPassword123")));
		return id;
	}
}
