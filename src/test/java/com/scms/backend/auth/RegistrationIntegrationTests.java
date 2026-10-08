package com.scms.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.TimeZone;
import java.util.UUID;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.account.MemberProfile;
import com.scms.backend.account.MemberProfileRepository;
import com.scms.backend.audit.AuditEventRepository;
import com.scms.backend.notification.NotificationDeliveryStatus;
import com.scms.backend.notification.NotificationRepository;
import jakarta.persistence.EntityManager;

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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class RegistrationIntegrationTests {

	private static final String REGISTER_PATH = "/api/v1/auth/register";
	private static final String PASSWORD = "Password123";

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
	private AuditEventRepository auditEventRepository;

	@Autowired
	private NotificationRepository notificationRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private Clock clock;

	@Test
	void validRegistrationCreatesCompleteAtomicResultWithoutAuthenticating() throws Exception {
		MvcResult result = performRegistration(request("  Nguyen Van A  ", "  0901234567  ",
			"  MEMBER@Example.COM  ", "https://cdn.example/member.png", "Run a marathon"))
			.andExpect(status().isCreated())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.accountId").isString())
			.andExpect(jsonPath("$.memberId", matchesPattern("MB-[0-9]+")))
			.andExpect(jsonPath("$.fullName").value("Nguyen Van A"))
			.andExpect(jsonPath("$.phone").value("0901234567"))
			.andExpect(jsonPath("$.email").value("member@example.com"))
			.andExpect(jsonPath("$.birthDate").value("2000-01-15"))
			.andExpect(jsonPath("$.role").value("MEMBER"))
			.andExpect(jsonPath("$.status").value("ACTIVE"))
			.andExpect(jsonPath("$.accessToken").doesNotExist())
			.andExpect(jsonPath("$.refreshToken").doesNotExist())
			.andExpect(jsonPath("$.password").doesNotExist())
			.andExpect(jsonPath("$.passwordHash").doesNotExist())
			.andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE))
			.andReturn();

		String responseBody = result.getResponse().getContentAsString();
		String accountIdText = com.jayway.jsonpath.JsonPath.read(responseBody, "$.accountId");
		String memberId = com.jayway.jsonpath.JsonPath.read(responseBody, "$.memberId");
		UUID accountId = UUID.fromString(accountIdText);
		entityManager.flush();
		entityManager.clear();

		Account account = accountRepository.findById(accountId).orElseThrow();
		assertThat(account.getRole()).isEqualTo(AccountRole.MEMBER);
		assertThat(account.getStatus()).isEqualTo(AccountStatus.ACTIVE);
		assertThat(account.getFullName()).isEqualTo("Nguyen Van A");
		assertThat(account.getPhone()).isEqualTo("0901234567");
		assertThat(account.getEmail()).isEqualTo("member@example.com");
		assertThat(account.getPasswordHash()).startsWith("$2").doesNotContain(PASSWORD);
		assertThat(passwordEncoder.matches(PASSWORD, account.getPasswordHash())).isTrue();

		MemberProfile profile = memberProfileRepository.findById(accountId).orElseThrow();
		assertThat(profile.getMemberCode()).isEqualTo(memberId).matches("MB-[0-9]+");
		assertThat(profile.getProfileImageUrl()).isEqualTo("https://cdn.example/member.png");
		assertThat(profile.getFitnessGoal()).isEqualTo("Run a marathon");
		assertThat(profile.getEmergencyContactName()).isNull();
		assertThat(profile.getEmergencyContactPhone()).isNull();

		assertThat(auditEventRepository.findAll()).singleElement().satisfies(event -> {
			assertThat(event.getActorAccountId()).isNull();
			assertThat(event.getAction()).isEqualTo("ACCOUNT_CREATED");
			assertThat(event.getTargetType()).isEqualTo("ACCOUNT");
			assertThat(event.getTargetId()).isEqualTo(accountId);
			assertThat(event.getBeforeData()).isNull();
			assertThat(event.getAfterData()).containsEntry("memberId", memberId);
			assertThat(event.getAfterData().toString()).doesNotContain(PASSWORD).doesNotContain(account.getPasswordHash());
		});
		assertThat(notificationRepository.findAll()).singleElement().satisfies(notification -> {
			assertThat(notification.getRecipientAccountId()).isEqualTo(accountId);
			assertThat(notification.getEventKey()).isEqualTo("ACCOUNT_CREATED:" + accountId);
			assertThat(notification.getNotificationType()).isEqualTo("ACCOUNT_CREATED");
			assertThat(notification.getTargetType()).isEqualTo("ACCOUNT");
			assertThat(notification.getTargetId()).isEqualTo(accountId);
			assertThat(notification.getDeliveryStatus()).isEqualTo(NotificationDeliveryStatus.PENDING);
			assertThat(notification.getDeliveryAttemptCount()).isZero();
			assertThat(notification.getPayload()).containsEntry("memberId", memberId);
			assertThat(notification.getPayload().toString()).doesNotContain(PASSWORD)
				.doesNotContain(account.getPasswordHash());
		});
		assertThat(responseBody).doesNotContain(PASSWORD).doesNotContain(account.getPasswordHash());
		assertThat(databaseTextFor(accountId)).doesNotContain(PASSWORD);
	}

	@Test
	void optionalFieldsMayBeOmittedAndRegisteredPhoneCanStillLogin() throws Exception {
		MvcResult result = performRegistration("""
			{
			  "fullName": "Optional Member",
			  "phone": "0912345678",
			  "email": "optional@example.com",
			  "password": "Password123",
			  "birthDate": "1999-05-20"
			}
			""")
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.memberId", matchesPattern("MB-[0-9]+")))
			.andReturn();

		String accountIdText = com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.accountId");
		MemberProfile profile = memberProfileRepository.findById(UUID.fromString(accountIdText)).orElseThrow();
		assertThat(profile.getProfileImageUrl()).isNull();
		assertThat(profile.getFitnessGoal()).isNull();
		assertThat(profile.getEmergencyContactName()).isNull();
		assertThat(profile.getEmergencyContactPhone()).isNull();

		mockMvc.perform(post("/api/v1/auth/login")
			.contextPath("/api/v1")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"identifier\":\"0912345678\",\"password\":\"Password123\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.accountId").value(accountIdText));
	}

	@Test
	void clientSuppliedMemberIdIsRejectedWithoutCreatingData() throws Exception {
		performRegistration("""
			{"fullName":"Member","phone":"0912345678","email":"member-id@example.com",
			 "password":"Password123","birthDate":"1999-05-20","memberId":"MB-1"}
			""")
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));

		assertCounts(0, 0, 0, 0);
	}

	@Test
	void missingBlankAndInvalidFieldsReturnValidationProblemWithoutPartialData() throws Exception {
		long accountsBefore = accountRepository.count();
		long profilesBefore = memberProfileRepository.count();
		long auditsBefore = auditEventRepository.count();
		long notificationsBefore = notificationRepository.count();

		performRegistration("{}")
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
			.andExpect(jsonPath("$.errors.fullName").exists())
			.andExpect(jsonPath("$.errors.phone").exists())
			.andExpect(jsonPath("$.errors.email").exists())
			.andExpect(jsonPath("$.errors.birthDate").exists());

		performRegistration("""
			{"fullName":" ","phone":" ","email":" ","password":" ","birthDate":null}
			""")
			.andExpect(status().isBadRequest())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
			.andExpect(jsonPath("$.errors.fullName").exists())
			.andExpect(jsonPath("$.errors.phone").exists())
			.andExpect(jsonPath("$.errors.email").exists())
			.andExpect(jsonPath("$.errors.birthDate").exists());

		performRegistration("""
			{"fullName":"Valid Name","phone":"0901234567","email":"valid@example.com",
			 "birthDate":"2000-01-15"}
			""")
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
			.andExpect(jsonPath("$.errors.password").exists());

		performRegistration(request("Valid Name", "0901234567", "bad-email", null, null))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
			.andExpect(jsonPath("$.errors.email").exists());
		performRegistration(request("Valid Name", "+84901234567", "valid@example.com", null, null))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors.phone").exists());
		performRegistration(requestWithPassword("Valid Name", "0901234567", "valid@example.com", "short"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors.password").exists());

		String futureBirthDate = LocalDate.now(clock).plusDays(1).toString();
		performRegistration("""
			{"fullName":"Valid Name","phone":"0901234567","email":"valid@example.com",
			 "password":"Password123","birthDate":"%s"}
			""".formatted(futureBirthDate))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
			.andExpect(jsonPath("$.errors.birthDate[0]").value("must not be in the future"));

		assertCounts(accountsBefore, profilesBefore, auditsBefore, notificationsBefore);
	}

	@Test
	void currentEmailAndPhoneConflictsReturnStable409CodesWithoutPartialData() throws Exception {
		Account emailOwner = createAccount(AccountRole.MEMBER, AccountStatus.ACTIVE, "0900000001",
			"duplicate@example.com");
		Account phoneOwner = createAccount(AccountRole.MEMBER, AccountStatus.SUSPENDED, "0900000002",
			"phone-owner@example.com");
		long accountsBefore = accountRepository.count();
		long profilesBefore = memberProfileRepository.count();
		long auditsBefore = auditEventRepository.count();
		long notificationsBefore = notificationRepository.count();

		performRegistration(request("Duplicate Email", "0900000003", "  DUPLICATE@EXAMPLE.COM  ", null, null))
			.andExpect(status().isConflict())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.code").value("EMAIL_ALREADY_EXISTS"));
		performRegistration(request("Duplicate Phone", "  " + phoneOwner.getPhone() + "  ",
			"new@example.com", null, null))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("PHONE_ALREADY_EXISTS"));

		assertThat(emailOwner.getStatus()).isEqualTo(AccountStatus.ACTIVE);
		assertCounts(accountsBefore, profilesBefore, auditsBefore, notificationsBefore);
	}

	@Test
	void identifiersFromInactiveAccountCanBeReused() throws Exception {
		createAccount(AccountRole.RECEPTIONIST, AccountStatus.INACTIVE, "0900000004", "inactive@example.com");

		performRegistration(request("Replacement Member", "0900000004", "inactive@example.com", null, null))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.role").value("MEMBER"))
			.andExpect(jsonPath("$.status").value("ACTIVE"));

		assertThat(accountRepository.count()).isEqualTo(2);
		assertThat(memberProfileRepository.count()).isEqualTo(1);
		assertThat(auditEventRepository.count()).isEqualTo(1);
		assertThat(notificationRepository.count()).isEqualTo(1);
	}

	private org.springframework.test.web.servlet.ResultActions performRegistration(String body) throws Exception {
		return mockMvc.perform(post(REGISTER_PATH)
			.contextPath("/api/v1")
			.contentType(MediaType.APPLICATION_JSON)
			.content(body));
	}

	private String request(String fullName, String phone, String email, String profileImageUrl, String fitnessGoal) {
		String optionalFields = profileImageUrl == null ? "" : """
			,"profileImageUrl":"%s","fitnessGoal":"%s"
			""".formatted(profileImageUrl, fitnessGoal);
		return """
			{"fullName":"%s","phone":"%s","email":"%s","password":"%s",
			 "birthDate":"2000-01-15"%s}
			""".formatted(fullName, phone, email, PASSWORD, optionalFields);
	}

	private String requestWithPassword(String fullName, String phone, String email, String password) {
		return """
			{"fullName":"%s","phone":"%s","email":"%s","password":"%s",
			 "birthDate":"2000-01-15"}
			""".formatted(fullName, phone, email, password);
	}

	private Account createAccount(AccountRole role, AccountStatus status, String phone, String email) {
		UUID id = UUID.randomUUID();
		return accountRepository.saveAndFlush(new Account(id, role, status, "Existing Account", phone, email,
			LocalDate.of(1990, 1, 1), passwordEncoder.encode("existing-password")));
	}

	private void assertCounts(long accounts, long profiles, long audits, long notifications) {
		assertThat(accountRepository.count()).isEqualTo(accounts);
		assertThat(memberProfileRepository.count()).isEqualTo(profiles);
		assertThat(auditEventRepository.count()).isEqualTo(audits);
		assertThat(notificationRepository.count()).isEqualTo(notifications);
	}

	private String databaseTextFor(UUID accountId) {
		return jdbcTemplate.queryForObject("""
			select a.password_hash || ' ' || coalesce(e.after_data::text, '') || ' ' || coalesce(n.payload::text, '')
			from accounts a
			left join audit_events e on e.target_id = a.id
			left join notifications n on n.recipient_account_id = a.id
			where a.id = ?
			""", String.class, accountId);
	}
}
