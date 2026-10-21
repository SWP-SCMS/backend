package com.scms.backend.emailverification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.audit.AuditEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties={
	"scms.email-verification.enabled=true",
	"scms.email-verification.hmac-active-version=v1",
	"scms.email-verification.hmac-keys=v1:MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=",
	"scms.email-verification.encryption-active-version=v1",
	"scms.email-verification.encryption-keys=v1:YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXpBQkNERUY=",
	"scms.email-verification.rate-limit-key=cmF0ZS1saW1pdC1rZXktMzItYnl0ZXMtbG9uZyEhISE=",
	"scms.email-verification.resend-cooldown=0s",
	"spring.mail.host=localhost",
	"scms.email.from=no-reply@example.test",
	"scms.email-verification.dispatch-delay=3600000",
	"scms.email-verification.dispatch-initial-delay=3600000"
})
@Transactional
class StaffEmailChangeIntegrationTests {
	static { TimeZone.setDefault(TimeZone.getTimeZone("UTC")); }
	@Container @ServiceConnection static final PostgreSQLContainer postgres=new PostgreSQLContainer("postgres:17");
	@Autowired StaffEmailChangeService service;@Autowired AccountRepository accounts;@Autowired JdbcTemplate db;
	@Autowired PasswordEncoder passwords;@Autowired EmailVerificationCrypto crypto;
	@Autowired AuditEventRepository audits;

	@Test
	void currentEmailRemainsUntilOtpVerificationThenRefreshTokensAreRevoked() {
		Account account=createStaff("old@example.test");insertRefreshToken(account.getId());
		StaffEmailChangeResponse pending=service.begin(account.getId(),
			new StaffEmailChangeRequest("NEW@example.test","Secret123!"));
		assertThat(pending.maskedEmail()).isEqualTo("n***@example.test");
		assertThat(accounts.findById(account.getId()).orElseThrow().getEmail()).isEqualTo("old@example.test");
		assertThat(service.status(account.getId()).pending()).isTrue();
		String otp=latestOtp(account.getId());

		StaffEmailChangeVerifyResponse verified=service.verify(account.getId(),new StaffEmailChangeVerifyRequest(otp));
		assertThat(verified).isEqualTo(new StaffEmailChangeVerifyResponse(true,"new@example.test"));
		assertThat(accounts.findById(account.getId()).orElseThrow().getEmail()).isEqualTo("new@example.test");
		assertThat(accounts.findById(account.getId()).orElseThrow().getAuthVersion()).isEqualTo(1L);
		assertThat(db.queryForObject("select email_verified_at is not null from accounts where id=?",Boolean.class,account.getId())).isTrue();
		assertThat(db.queryForObject("select revoked_at is not null from refresh_tokens where account_id=?",Boolean.class,account.getId())).isTrue();
		assertThat(service.status(account.getId()).pending()).isFalse();
		audits.flush();
		String audit=db.queryForObject("select after_data::text from audit_events where action='STAFF_EMAIL_CHANGE_VERIFIED' and target_id=?",String.class,account.getId());
		assertThat(audit).contains("n***@example.test").doesNotContain(otp);
		assertThat(service.verify(account.getId(),new StaffEmailChangeVerifyRequest(otp)).verified()).isTrue();
	}

	@Test
	void wrongOtpCountsAttemptsAndCancelKeepsCurrentEmail() {
		Account account=createStaff("cancel-old@example.test");
		service.begin(account.getId(),new StaffEmailChangeRequest("cancel-new@example.test","Secret123!"));
		String otp=latestOtp(account.getId());String invalid=(otp.charAt(0)=='0'?"1":"0")+otp.substring(1);
		assertThatThrownBy(()->service.verify(account.getId(),new StaffEmailChangeVerifyRequest(invalid)))
			.isInstanceOfSatisfying(EmailVerificationException.class,
				exception->assertThat(exception.getCode()).isEqualTo("INVALID_OR_EXPIRED_OTP"));
		assertThat(db.queryForObject("select attempt_count from staff_email_change_requests where account_id=?",Integer.class,account.getId())).isEqualTo(1);
		service.cancel(account.getId());
		assertThat(accounts.findById(account.getId()).orElseThrow().getEmail()).isEqualTo("cancel-old@example.test");
		assertThat(db.queryForObject("select status from staff_email_change_requests where account_id=?",String.class,account.getId())).isEqualTo("CANCELLED");
		assertThat(db.queryForObject("select status from email_outbox where account_id=? and template_key='STAFF_EMAIL_CHANGE_OTP'",String.class,account.getId())).isEqualTo("CANCELLED");
	}

	@Test
	void resendSupersedesOldOtpAndKeepsOnlyLatestValid() {
		Account account=createStaff("resend-old@example.test");
		service.begin(account.getId(),new StaffEmailChangeRequest("resend-new@example.test","Secret123!"));
		service.resend(account.getId());String newOtp=latestOtp(account.getId());
		assertThat(db.queryForObject("select generation from staff_email_change_requests where account_id=?",Integer.class,account.getId())).isEqualTo(2);
		assertThat(db.queryForObject("select count(*) from email_outbox where account_id=? and template_key='STAFF_EMAIL_CHANGE_OTP' and status='CANCELLED'",Long.class,account.getId())).isEqualTo(1L);
		service.verify(account.getId(),new StaffEmailChangeVerifyRequest(newOtp));
		assertThat(accounts.findById(account.getId()).orElseThrow().getEmail()).isEqualTo("resend-new@example.test");
	}

	private Account createStaff(String email){UUID id=UUID.randomUUID();return accounts.saveAndFlush(new Account(id,AccountRole.COACH,AccountStatus.ACTIVE,"Staff <safe>","09"+id.toString().substring(0,8),email,LocalDate.of(1990,1,1),passwords.encode("Secret123!")));}
	private void insertRefreshToken(UUID accountId){db.update("insert into refresh_tokens(id,account_id,token_hash,expires_at) values (?,?,?,?)",UUID.randomUUID(),accountId,"a".repeat(64),Timestamp.from(Instant.now().plusSeconds(3600)));}
	private String latestOtp(UUID accountId){Map<String,Object> row=db.queryForMap("select payload_ciphertext,payload_nonce,encryption_key_version from email_outbox where account_id=? and template_key='STAFF_EMAIL_CHANGE_OTP' and status='PENDING' order by created_at desc limit 1",accountId);return crypto.decryptOtp((byte[])row.get("payload_ciphertext"),(byte[])row.get("payload_nonce"),(String)row.get("encryption_key_version"));}
}
