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
	@Autowired AccountEmailChangeService service;@Autowired AccountRepository accounts;@Autowired JdbcTemplate db;
	@Autowired PasswordEncoder passwords;@Autowired EmailVerificationCrypto crypto;
	@Autowired AuditEventRepository audits;

	@Test
	void currentEmailRemainsUntilOtpVerificationThenRefreshTokensAreRevoked() {
		Account account=createAccount(AccountRole.MEMBER,"old@example.test");insertRefreshToken(account.getId());
		AccountEmailChangeResponse pending=service.begin(account.getId(),
			new AccountEmailChangeRequest("NEW@example.test","Secret123!"),"127.0.0.1");
		assertThat(pending.maskedEmail()).isEqualTo("n***@example.test");
		assertThat(accounts.findById(account.getId()).orElseThrow().getEmail()).isEqualTo("old@example.test");
		assertThat(service.status(account.getId()).pending()).isTrue();
		assertThat(db.queryForMap("select payload_format,delivery_provider from email_outbox where account_id=?",account.getId()))
			.containsEntry("payload_format","EMAIL_REQUEST_V1").containsEntry("delivery_provider","smtp");
		assertThat(db.queryForObject("select count(*) from notification_outbox where recipient_account_id=? and notification_type='ACCOUNT_EMAIL_CHANGE_REQUESTED'",Integer.class,account.getId())).isEqualTo(1);
		String otp=latestOtp(account.getId());

		AccountEmailChangeVerifyResponse verified=service.verify(account.getId(),new AccountEmailChangeVerifyRequest(otp));
		assertThat(verified).isEqualTo(new AccountEmailChangeVerifyResponse(true,"new@example.test"));
		assertThat(accounts.findById(account.getId()).orElseThrow().getEmail()).isEqualTo("new@example.test");
		assertThat(accounts.findById(account.getId()).orElseThrow().getAuthVersion()).isEqualTo(1L);
		assertThat(db.queryForObject("select email_verified_at is not null from accounts where id=?",Boolean.class,account.getId())).isTrue();
		assertThat(db.queryForObject("select revoked_at is not null from refresh_tokens where account_id=?",Boolean.class,account.getId())).isTrue();
		assertThat(service.status(account.getId()).pending()).isFalse();
		audits.flush();
		String audit=db.queryForObject("select after_data::text from audit_events where action='ACCOUNT_EMAIL_CHANGE_COMPLETED' and target_id=?",String.class,account.getId());
		assertThat(audit).contains("n***@example.test").doesNotContain(otp);
		assertThat(service.verify(account.getId(),new AccountEmailChangeVerifyRequest(otp)).verified()).isTrue();
		assertThat(db.queryForObject("select count(*) from notification_outbox where recipient_account_id=? and notification_type='ACCOUNT_EMAIL_CHANGE_COMPLETED'",Integer.class,account.getId())).isEqualTo(1);
	}

	@Test
	void wrongOtpCountsAttemptsAndCancelKeepsCurrentEmail() {
		Account account=createAccount(AccountRole.COACH,"cancel-old@example.test");
		service.begin(account.getId(),new AccountEmailChangeRequest("cancel-new@example.test","Secret123!"),"127.0.0.2");
		String otp=latestOtp(account.getId());String invalid=(otp.charAt(0)=='0'?"1":"0")+otp.substring(1);
		assertThatThrownBy(()->service.verify(account.getId(),new AccountEmailChangeVerifyRequest(invalid)))
			.isInstanceOfSatisfying(EmailVerificationException.class,
				exception->assertThat(exception.getCode()).isEqualTo("INVALID_OR_EXPIRED_OTP"));
		assertThat(db.queryForObject("select attempt_count from staff_email_change_requests where account_id=?",Integer.class,account.getId())).isEqualTo(1);
		service.cancel(account.getId());
		assertThat(accounts.findById(account.getId()).orElseThrow().getEmail()).isEqualTo("cancel-old@example.test");
		assertThat(db.queryForObject("select status from staff_email_change_requests where account_id=?",String.class,account.getId())).isEqualTo("CANCELLED");
		assertThat(db.queryForObject("select status from email_outbox where account_id=? and template_key='ACCOUNT_EMAIL_CHANGE_OTP'",String.class,account.getId())).isEqualTo("CANCELLED");
	}

	@Test
	void resendSupersedesOldOtpAndKeepsOnlyLatestValid() {
		Account account=createAccount(AccountRole.RECEPTIONIST,"resend-old@example.test");
		service.begin(account.getId(),new AccountEmailChangeRequest("resend-new@example.test","Secret123!"),"127.0.0.3");
		service.resend(account.getId());String newOtp=latestOtp(account.getId());
		assertThat(db.queryForObject("select generation from staff_email_change_requests where account_id=?",Integer.class,account.getId())).isEqualTo(2);
		assertThat(db.queryForObject("select count(*) from email_outbox where account_id=? and template_key='ACCOUNT_EMAIL_CHANGE_OTP' and status='CANCELLED'",Long.class,account.getId())).isEqualTo(1L);
		service.verify(account.getId(),new AccountEmailChangeVerifyRequest(newOtp));
		assertThat(accounts.findById(account.getId()).orElseThrow().getEmail()).isEqualTo("resend-new@example.test");
	}

	@Test
	void pendingEmailDoesNotReserveAddressAndReplaceSupersedesOldRequest() {
		Account first=createAccount(AccountRole.MEMBER,"first@example.test");
		Account second=createAccount(AccountRole.MANAGER,"second@example.test");
		Account third=createAccount(AccountRole.COACH,"third@example.test");
		service.begin(first.getId(),new AccountEmailChangeRequest("shared@example.test","Secret123!"),"127.0.0.4");
		service.begin(second.getId(),new AccountEmailChangeRequest("shared@example.test","Secret123!"),"127.0.0.5");
		service.verify(first.getId(),new AccountEmailChangeVerifyRequest(latestOtp(first.getId())));
		assertThatThrownBy(()->service.verify(second.getId(),new AccountEmailChangeVerifyRequest(latestOtp(second.getId()))))
			.isInstanceOfSatisfying(EmailVerificationException.class,
				exception->assertThat(exception.getCode()).isEqualTo("EMAIL_ALREADY_EXISTS"));
		assertThat(db.queryForObject("select status from staff_email_change_requests where account_id=?",String.class,second.getId())).isEqualTo("EXPIRED");
		service.begin(third.getId(),new AccountEmailChangeRequest("initial@example.test","Secret123!"),"127.0.0.6");
		service.replace(third.getId(),new AccountEmailChangeRequest("replacement@example.test","Secret123!"),"127.0.0.6");
		assertThat(db.queryForList("select status from staff_email_change_requests where account_id=? order by created_at",String.class,third.getId()))
			.containsExactly("SUPERSEDED","PENDING");
	}

	private Account createAccount(AccountRole role,String email){UUID id=UUID.randomUUID();return accounts.saveAndFlush(new Account(id,role,AccountStatus.ACTIVE,"Account <safe>","09"+id.toString().substring(0,8),email,LocalDate.of(1990,1,1),passwords.encode("Secret123!")));}
	private void insertRefreshToken(UUID accountId){db.update("insert into refresh_tokens(id,account_id,token_hash,expires_at) values (?,?,?,?)",UUID.randomUUID(),accountId,"a".repeat(64),Timestamp.from(Instant.now().plusSeconds(3600)));}
	private String latestOtp(UUID accountId){Map<String,Object> row=db.queryForMap("select payload_ciphertext,payload_nonce,encryption_key_version from email_outbox where account_id=? and template_key='ACCOUNT_EMAIL_CHANGE_OTP' and status='PENDING' order by created_at desc limit 1",accountId);String payload=crypto.decryptPayload((byte[])row.get("payload_ciphertext"),(byte[])row.get("payload_nonce"),(String)row.get("encryption_key_version"));var matcher=java.util.regex.Pattern.compile("\\b[0-9]{6}\\b").matcher(payload);if(!matcher.find())throw new AssertionError("OTP missing from encrypted email snapshot");return matcher.group();}
}
