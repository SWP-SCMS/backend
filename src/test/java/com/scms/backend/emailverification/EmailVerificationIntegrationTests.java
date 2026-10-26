package com.scms.backend.emailverification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.UUID;
import java.util.TimeZone;
import java.time.ZoneOffset;
import com.jayway.jsonpath.JsonPath;
import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.audit.AuditEventRepository;
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
@TestPropertySource(properties={
	"scms.email-verification.enabled=true",
	"scms.email-verification.hmac-active-version=v1",
	"scms.email-verification.hmac-keys=v1:MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=",
	"scms.email-verification.encryption-active-version=v1",
	"scms.email-verification.encryption-keys=v1:YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXpBQkNERUY=",
	"scms.email-verification.rate-limit-key=cmF0ZS1saW1pdC1rZXktMzItYnl0ZXMtbG9uZyEhISE=",
	"scms.email-verification.cookie-secure=false",
	"spring.mail.host=localhost",
	"scms.email.from=no-reply@example.test",
	"scms.email-verification.dispatch-delay=3600000",
	"scms.email-verification.dispatch-initial-delay=3600000"
})
@Transactional
class EmailVerificationIntegrationTests {
	static { TimeZone.setDefault(TimeZone.getTimeZone(ZoneOffset.UTC)); }
	@Container @ServiceConnection static final PostgreSQLContainer postgres=new PostgreSQLContainer("postgres:17");
	@Autowired MockMvc mvc;@Autowired JdbcTemplate db;@Autowired EmailVerificationCrypto crypto;
	@Autowired AccountRepository accounts;@Autowired PasswordEncoder passwords;@Autowired ManualActivationService manual;
	@Autowired AuditEventRepository audits;

	@Test
	void registerCreatesEncryptedPendingArtifactsAndOtpActivatesExactlyOnce() throws Exception {
		MvcResult registered=register("verify@example.test","0907654321")
			.andExpect(status().isCreated()).andExpect(jsonPath("$.verificationRequired").value(true))
			.andExpect(jsonPath("$.status").value("INACTIVE")).andExpect(jsonPath("$.accountId").doesNotExist())
			.andExpect(cookie().httpOnly(EmailVerificationController.COOKIE_NAME,true)).andReturn();
		String body=registered.getResponse().getContentAsString();String csrf=JsonPath.read(body,"$.csrfToken");
		Cookie pending=registered.getResponse().getCookie(EmailVerificationController.COOKIE_NAME);assertThat(csrf).isNotBlank();
		UUID accountId=db.queryForObject("select id from accounts where email='verify@example.test'",UUID.class);
		assertThat(db.queryForObject("select status from accounts where id=?",String.class,accountId)).isEqualTo("INACTIVE");
		assertThat(db.queryForObject("select email_verification_required from accounts where id=?",Boolean.class,accountId)).isTrue();
		assertThat(db.queryForObject("select payload_ciphertext is not null and payload_nonce is not null from email_outbox where account_id=?",Boolean.class,accountId)).isTrue();
		assertThat(db.queryForMap("select payload_format,delivery_provider from email_outbox where account_id=?",accountId))
			.containsEntry("payload_format","EMAIL_REQUEST_V1").containsEntry("delivery_provider","smtp");
		String databaseText=db.queryForObject("select otp_digest||encode(payload_ciphertext,'hex') from email_verification_challenges c join email_outbox o on o.account_id=c.account_id where c.account_id=?",String.class,accountId);
		assertThat(databaseText).doesNotContain("012345");
		db.update("update email_verification_challenges set otp_digest=? where account_id=?",
			crypto.otpDigest("012345","v1"),accountId);

		mvc.perform(post("/api/v1/auth/email-verification/verify").contextPath("/api/v1")
			.cookie(pending).contentType(MediaType.APPLICATION_JSON)
			.content("{\"email\":\"verify@example.test\",\"otp\":\"012345\"}"))
			.andExpect(status().isOk()).andExpect(jsonPath("$.verified").value(true))
			.andExpect(jsonPath("$.status").value("ACTIVE"));
		assertThat(db.queryForObject("select status from accounts where id=?",String.class,accountId)).isEqualTo("ACTIVE");
		assertThat(db.queryForObject("select email_verified_at is not null from accounts where id=?",Boolean.class,accountId)).isTrue();
		long auditCount=audits.findAll().stream().filter(e->accountId.equals(e.getTargetId())&&"EMAIL_VERIFIED".equals(e.getAction())).count();
		mvc.perform(post("/api/v1/auth/email-verification/verify").contextPath("/api/v1")
			.contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"verify@example.test\",\"otp\":\"012345\"}"))
			.andExpect(status().isOk());
		assertThat(audits.findAll().stream().filter(e->accountId.equals(e.getTargetId())&&"EMAIL_VERIFIED".equals(e.getAction())).count()).isEqualTo(auditCount);
	}

	@Test
	void changeEmailRequiresCookieCsrfAndOriginAndRotatesSession() throws Exception {
		MvcResult registered=register("old@example.test","0907654322").andExpect(status().isCreated()).andReturn();
		String csrf=JsonPath.read(registered.getResponse().getContentAsString(),"$.csrfToken");Cookie cookie=registered.getResponse().getCookie(EmailVerificationController.COOKIE_NAME);
		mvc.perform(post("/api/v1/auth/email-verification/change-email").contextPath("/api/v1")
			.cookie(cookie).header(HttpHeaders.ORIGIN,"http://localhost:5173").contentType(MediaType.APPLICATION_JSON)
			.content("{\"newEmail\":\"new@example.test\"}" )).andExpect(status().isUnauthorized());
		MvcResult changed=mvc.perform(post("/api/v1/auth/email-verification/change-email").contextPath("/api/v1")
			.cookie(cookie).header(HttpHeaders.ORIGIN,"http://localhost:5173").header("X-Pending-CSRF",csrf)
			.contentType(MediaType.APPLICATION_JSON).content("{\"newEmail\":\"new@example.test\"}"))
			.andExpect(status().isOk()).andExpect(jsonPath("$.changed").value(true))
			.andExpect(jsonPath("$.maskedEmail").value("n***@example.test")).andReturn();
		assertThat(changed.getResponse().getCookie(EmailVerificationController.COOKIE_NAME).getValue()).isNotEqualTo(cookie.getValue());
		assertThat(db.queryForObject("select count(*) from accounts where email='new@example.test'",Integer.class)).isEqualTo(1);
	}

	@Test
	void receptionistRequestAndManagerApprovalActivatesWithoutMarkingEmailVerified() throws Exception {
		register("manual@example.test","0907654323").andExpect(status().isCreated());
		UUID receptionist=createActor(AccountRole.RECEPTIONIST,"0911111101","reception@example.test");
		UUID manager=createActor(AccountRole.MANAGER,"0911111102","manager@example.test");
		String memberId=db.queryForObject("select p.member_code from member_profiles p join accounts a on a.id=p.account_id where a.email='manual@example.test'",String.class);
		ManualActivationResponse request=manual.create(receptionist,new ManualActivationCreateRequest(memberId,
			ManualActivationReasonCode.MEMBER_CANNOT_ACCESS_EMAIL,"Mailbox unavailable",
			ManualVerificationMethod.IN_PERSON_GOVERNMENT_ID_CHECK,"Original document checked in person",true));
		assertThat(request.requestedByName()).isEqualTo("RECEPTIONIST");
		assertThat(request.requestedByRole()).isEqualTo(AccountRole.RECEPTIONIST);
		db.update("update accounts set full_name='Renamed Receptionist',status='INACTIVE' where id=?",receptionist);
		ManualActivationResponse historical=manual.getForActor(manager,request.requestId(),AccountRole.MANAGER);
		assertThat(historical.requestedByName()).isEqualTo("RECEPTIONIST");
		assertThat(historical.requestedByRole()).isEqualTo(AccountRole.RECEPTIONIST);
		assertThat(manual.list(manager,AccountRole.MANAGER,ManualActivationStatus.PENDING,0,20).content())
			.extracting(ManualActivationResponse::requestedByName).contains("RECEPTIONIST");
		ManualActivationResponse approved=manual.approve(manager,request.requestId(),new ManualActivationDecisionRequest("Evidence reviewed"));
		assertThat(approved.status()).isEqualTo(ManualActivationStatus.APPROVED);
		assertThat(db.queryForObject("select status from accounts where id=?",String.class,approved.accountId())).isEqualTo("ACTIVE");
		assertThat(db.queryForObject("select email_verified_at is null from accounts where id=?",Boolean.class,approved.accountId())).isTrue();
		assertThat(audits.findAll().stream().map(e->e.getAction())).contains("MANUAL_ACTIVATION_REQUEST_APPROVED",
			"EMAIL_VERIFICATION_BYPASSED","ACCOUNT_ACTIVATED_MANUALLY").doesNotContain("EMAIL_VERIFIED");
	}

	private org.springframework.test.web.servlet.ResultActions register(String email,String phone)throws Exception{return mvc.perform(post("/api/v1/auth/register").contextPath("/api/v1").contentType(MediaType.APPLICATION_JSON).content("""
		{"fullName":"Email Member","phone":"%s","email":"%s","password":"Password123","birthDate":"2000-01-01"}
		""".formatted(phone,email)));}
	private UUID createActor(AccountRole role,String phone,String email){UUID id=UUID.randomUUID();accounts.saveAndFlush(new Account(id,role,AccountStatus.ACTIVE,role.name(),phone,email,LocalDate.of(1990,1,1),passwords.encode("Password123")));return id;}
}
