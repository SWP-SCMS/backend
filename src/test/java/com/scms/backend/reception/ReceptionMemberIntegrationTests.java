package com.scms.backend.reception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import java.util.stream.Stream;

import com.jayway.jsonpath.JsonPath;
import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.account.MemberProfile;
import com.scms.backend.account.MemberProfileRepository;
import com.scms.backend.audit.AuditEvent;
import com.scms.backend.audit.AuditEventRepository;
import com.scms.backend.notification.Notification;
import com.scms.backend.notification.NotificationDeliveryStatus;
import com.scms.backend.notification.NotificationRepository;
import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
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
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class ReceptionMemberIntegrationTests {

	private static final String CREATE_MEMBER_PATH = "/api/v1/reception/members";
	private static final String EXISTING_PASSWORD = "existing-password";

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
	private JwtEncoder jwtEncoder;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private Clock clock;

	@Test
	void requestStringRepresentationRedactsDefaultPasswordSource() {
		ReceptionMemberCreateRequest request = new ReceptionMemberCreateRequest("Member", "0901234567",
			"member@redaction.test", LocalDate.of(2000, 1, 1), null);

		assertThat(request.toString()).contains("phone=[REDACTED]").doesNotContain("0901234567");
	}

	@Test
	void activeReceptionistCreatesNormalizedMemberAuditAndDurableNotification() throws Exception {
		Account receptionist = createAccount(AccountRole.RECEPTIONIST, AccountStatus.ACTIVE);
		long accountsBefore = accountRepository.count();
		long profilesBefore = memberProfileRepository.count();
		long auditsBefore = auditEventRepository.count();
		long notificationsBefore = notificationRepository.count();

		MvcResult result = createMember(accessToken(receptionist), """
			{
			  "fullName": "  Nguyen Van A  ",
			  "phone": "  0901234567  ",
			  "email": "  MEMBER@Example.COM  ",
			  "birthDate": "2000-01-01",
			  "profileImageUrl": "https://cdn.example/member.png"
			}
			""")
			.andExpect(status().isCreated())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.accountId").isString())
			.andExpect(jsonPath("$.memberId", matchesPattern("MB-[0-9]+")))
			.andExpect(jsonPath("$.fullName").value("Nguyen Van A"))
			.andExpect(jsonPath("$.phone").value("0901234567"))
			.andExpect(jsonPath("$.email").value("member@example.com"))
			.andExpect(jsonPath("$.birthDate").value("2000-01-01"))
			.andExpect(jsonPath("$.role").value("MEMBER"))
			.andExpect(jsonPath("$.status").value("ACTIVE"))
			.andExpect(jsonPath("$.password").doesNotExist())
			.andExpect(jsonPath("$.defaultPassword").doesNotExist())
			.andExpect(jsonPath("$.passwordHash").doesNotExist())
			.andExpect(jsonPath("$.accessToken").doesNotExist())
			.andExpect(jsonPath("$.refreshToken").doesNotExist())
			.andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE))
			.andReturn();

		String body = result.getResponse().getContentAsString();
		UUID accountId = UUID.fromString(JsonPath.read(body, "$.accountId"));
		String memberId = JsonPath.read(body, "$.memberId");
		Account member = accountRepository.findById(accountId).orElseThrow();
		assertThat(member.getRole()).isEqualTo(AccountRole.MEMBER);
		assertThat(member.getStatus()).isEqualTo(AccountStatus.ACTIVE);
		assertThat(member.getFullName()).isEqualTo("Nguyen Van A");
		assertThat(member.getPhone()).isEqualTo("0901234567");
		assertThat(member.getEmail()).isEqualTo("member@example.com");
		assertThat(member.getPasswordHash()).startsWith("$2").isNotEqualTo(member.getPhone());
		assertThat(passwordEncoder.matches(member.getPhone(), member.getPasswordHash())).isTrue();

		MemberProfile profile = memberProfileRepository.findById(accountId).orElseThrow();
		assertThat(profile.getMemberCode()).isEqualTo(memberId);
		assertThat(profile.getProfileImageUrl()).isEqualTo("https://cdn.example/member.png");
		assertThat(profile.getFitnessGoal()).isNull();
		assertThat(profile.getEmergencyContactName()).isNull();
		assertThat(profile.getEmergencyContactPhone()).isNull();

		AuditEvent audit = auditFor(accountId);
		assertThat(audit.getActorAccountId()).isEqualTo(receptionist.getId());
		assertThat(audit.getAction()).isEqualTo("ACCOUNT_CREATED");
		assertThat(audit.getTargetType()).isEqualTo("ACCOUNT");
		assertThat(audit.getTargetId()).isEqualTo(accountId);
		assertThat(audit.getAfterData()).containsEntry("memberId", memberId)
			.doesNotContainKeys("password", "defaultPassword", "passwordHash", "credential", "phone");
		assertThat(audit.getAfterData().toString()).doesNotContain(member.getPasswordHash())
			.doesNotContain(member.getPhone());

		Notification notification = notificationFor(accountId);
		assertThat(notification.getRecipientAccountId()).isEqualTo(accountId);
		assertThat(notification.getEventKey()).isEqualTo("ACCOUNT_CREATED:" + accountId);
		assertThat(notification.getNotificationType()).isEqualTo("ACCOUNT_CREATED");
		assertThat(notification.getTargetType()).isEqualTo("ACCOUNT");
		assertThat(notification.getTargetId()).isEqualTo(accountId);
		assertThat(notification.getPayload()).containsExactly(Map.entry("memberId", memberId));
		assertThat(notification.getDeliveryStatus()).isEqualTo(NotificationDeliveryStatus.PENDING);
		assertThat(notification.getDeliveryAttemptCount()).isZero();

		assertCounts(accountsBefore + 1, profilesBefore + 1, auditsBefore + 1, notificationsBefore + 1);
		assertThat(body).doesNotContain(member.getPasswordHash());
	}

	@Test
	void optionalProfileImageMayBeOmittedOrNull() throws Exception {
		Account receptionist = createAccount(AccountRole.RECEPTIONIST, AccountStatus.ACTIVE);
		MvcResult omitted = createMember(accessToken(receptionist), validRequest("0901000001", "omitted@example.com", ""))
			.andExpect(status().isCreated()).andReturn();
		MvcResult explicitNull = createMember(accessToken(receptionist),
			validRequest("0901000002", "null@example.com", ",\"profileImageUrl\":null"))
			.andExpect(status().isCreated()).andReturn();

		UUID omittedId = UUID.fromString(JsonPath.read(omitted.getResponse().getContentAsString(), "$.accountId"));
		UUID nullId = UUID.fromString(JsonPath.read(explicitNull.getResponse().getContentAsString(), "$.accountId"));
		assertThat(memberProfileRepository.findById(omittedId).orElseThrow().getProfileImageUrl()).isNull();
		assertThat(memberProfileRepository.findById(nullId).orElseThrow().getProfileImageUrl()).isNull();
	}

	@Test
	void missingAndInvalidBearerTokensAreUnauthorized() throws Exception {
		mockMvc.perform(post(CREATE_MEMBER_PATH).contextPath("/api/v1")
			.contentType(MediaType.APPLICATION_JSON)
			.content(validRequest("0901000010", "missing-token@example.com", "")))
			.andExpect(status().isUnauthorized());
		createMember("not-a-jwt", validRequest("0901000011", "invalid-token@example.com", ""))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void expiredTokenIsUnauthorized() throws Exception {
		Account receptionist = createAccount(AccountRole.RECEPTIONIST, AccountStatus.ACTIVE);

		createMember(accessToken(receptionist, Instant.now().minusSeconds(300)),
			validRequest("0901000012", "expired@example.com", ""))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void receptionistTokenIsRejectedAfterAccountBecomesInactive() throws Exception {
		Account receptionist = createAccount(AccountRole.RECEPTIONIST, AccountStatus.ACTIVE);
		String token = accessToken(receptionist);
		jdbcTemplate.update("update accounts set status = 'INACTIVE' where id = ?", receptionist.getId());
		entityManager.clear();

		createMember(token, validRequest("0901000013", "inactive-actor@example.com", ""))
			.andExpect(status().isUnauthorized());
	}

	@ParameterizedTest
	@EnumSource(value = AccountRole.class, names = {"MEMBER", "COACH", "MANAGER"})
	void otherRolesAreForbidden(AccountRole role) throws Exception {
		Account account = createAccount(role, AccountStatus.ACTIVE);

		createMember(accessToken(account), validRequest(phoneFor(role), role.name().toLowerCase() + "@forbidden.test", ""))
			.andExpect(status().isForbidden());
	}

	@ParameterizedTest(name = "invalid required input {0}")
	@MethodSource("invalidRequiredRequests")
	void missingOrBlankRequiredFieldsReturnValidationError(String ignored, String body) throws Exception {
		Account receptionist = createAccount(AccountRole.RECEPTIONIST, AccountStatus.ACTIVE);
		long accountsBefore = accountRepository.count();

		createMember(accessToken(receptionist), body)
			.andExpect(status().isBadRequest())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
		assertThat(accountRepository.count()).isEqualTo(accountsBefore);
	}

	@ParameterizedTest
	@MethodSource("invalidFormattedRequests")
	void invalidFormatsReturnValidationError(String body) throws Exception {
		Account receptionist = createAccount(AccountRole.RECEPTIONIST, AccountStatus.ACTIVE);

		createMember(accessToken(receptionist), body)
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
	}

	@Test
	void futureBirthDateUsesConfiguredClock() throws Exception {
		Account receptionist = createAccount(AccountRole.RECEPTIONIST, AccountStatus.ACTIVE);
		String body = validRequest("0901000020", "future@example.com", "")
			.replace("2000-01-01", LocalDate.now(clock).plusDays(1).toString());

		createMember(accessToken(receptionist), body)
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
			.andExpect(jsonPath("$.errors.birthDate[0]").value("must not be in the future"));
	}

	@ParameterizedTest
	@ValueSource(strings = {"password", "role", "status", "accountId", "memberId", "fitnessGoal",
		"emergencyContactName", "emergencyContactPhone", "unknown"})
	void readOnlyAndUnknownFieldsReturnMalformedRequest(String field) throws Exception {
		Account receptionist = createAccount(AccountRole.RECEPTIONIST, AccountStatus.ACTIVE);
		String body = validRequest("0901000021", field + "@malformed.test", ",\"" + field + "\":\"forbidden\"");

		createMember(accessToken(receptionist), body)
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
	}

	@Test
	void duplicateCurrentEmailAndPhoneReturnSpecificConflictCodes() throws Exception {
		Account receptionist = createAccount(AccountRole.RECEPTIONIST, AccountStatus.ACTIVE);
		createAccount(AccountRole.MEMBER, AccountStatus.ACTIVE, "0901000030", "duplicate-us15@example.com");
		createAccount(AccountRole.MEMBER, AccountStatus.SUSPENDED, "0901000031", "phone-owner-us15@example.com");

		createMember(accessToken(receptionist),
			validRequest("0901000032", "  DUPLICATE-US15@EXAMPLE.COM  ", ""))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("EMAIL_ALREADY_EXISTS"));
		createMember(accessToken(receptionist),
			validRequest("  0901000031  ", "new-us15@example.com", ""))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("PHONE_ALREADY_EXISTS"));
	}

	@Test
	void databaseIdentifierRaceIsTranslatedToConflict() throws Exception {
		Account receptionist = createAccount(AccountRole.RECEPTIONIST, AccountStatus.ACTIVE);
		installFailureTrigger("accounts", "us15_account_race", "us15_force_account_race",
			"duplicate key value violates unique constraint \\\"uq_accounts_current_email\\\"", "23505");
		try {
			createMember(accessToken(receptionist), validRequest("0901000040", "race-us15@example.com", ""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("EMAIL_ALREADY_EXISTS"));
		}
		finally {
			dropFailureTrigger("accounts", "us15_account_race", "us15_force_account_race");
		}
	}

	@Test
	void identifiersFromInactiveStaffAccountCanBeReusedWithoutChangingOldAccount() throws Exception {
		Account receptionist = createAccount(AccountRole.RECEPTIONIST, AccountStatus.ACTIVE);
		Account inactive = createAccount(AccountRole.COACH, AccountStatus.INACTIVE, "0901000050",
			"inactive-us15@example.com");

		createMember(accessToken(receptionist),
			validRequest("0901000050", "inactive-us15@example.com", ""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.role").value("MEMBER"));

		Account unchanged = accountRepository.findById(inactive.getId()).orElseThrow();
		assertThat(unchanged.getRole()).isEqualTo(AccountRole.COACH);
		assertThat(unchanged.getStatus()).isEqualTo(AccountStatus.INACTIVE);
	}

	@Test
	void auditFailureRollsBackAccountProfileAndSkipsNotification() throws Exception {
		Account receptionist = createAccount(AccountRole.RECEPTIONIST, AccountStatus.ACTIVE);
		long accountsBefore = accountRepository.count();
		long profilesBefore = memberProfileRepository.count();
		long auditsBefore = auditEventRepository.count();
		long notificationsBefore = notificationRepository.count();
		installFailureTrigger("audit_events", "us15_audit_failure", "us15_force_audit_failure",
			"forced audit failure", "P0001");
		try {
			createMember(accessToken(receptionist), validRequest("0901000060", "audit-failure-us15@example.com", ""))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.code").value("INTERNAL_ERROR"));
		}
		finally {
			dropFailureTrigger("audit_events", "us15_audit_failure", "us15_force_audit_failure");
		}

		assertCounts(accountsBefore, profilesBefore, auditsBefore, notificationsBefore);
		assertThat(accountRepository.findByEmailIgnoreCaseAndStatus("audit-failure-us15@example.com",
			AccountStatus.ACTIVE)).isEmpty();
	}

	@Test
	void notificationFailureRollsBackMemberProfileAndAudit() throws Exception {
		Account receptionist = createAccount(AccountRole.RECEPTIONIST, AccountStatus.ACTIVE);
		long accountsBefore = accountRepository.count();
		long profilesBefore = memberProfileRepository.count();
		long auditsBefore = auditEventRepository.count();
		long notificationsBefore = notificationRepository.count();
		installFailureTrigger("notifications", "us15_notification_failure", "us15_force_notification_failure",
			"forced notification failure", "P0001");
		try {
			createMember(accessToken(receptionist),
				validRequest("0901000070", "notification-failure-us15@example.com", ""))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.code").value("INTERNAL_ERROR"));
		}
		finally {
			dropFailureTrigger("notifications", "us15_notification_failure", "us15_force_notification_failure");
		}

		assertCounts(accountsBefore, profilesBefore, auditsBefore, notificationsBefore);
		assertThat(accountRepository.findByEmailIgnoreCaseAndStatus("notification-failure-us15@example.com",
			AccountStatus.ACTIVE)).isEmpty();
	}

	private static Stream<Arguments> invalidRequiredRequests() {
		return Stream.of(
			Arguments.of("missing fullName", "{\"phone\":\"0901111111\",\"email\":\"a1@test.com\",\"birthDate\":\"2000-01-01\"}"),
			Arguments.of("blank fullName", "{\"fullName\":\" \",\"phone\":\"0901111112\",\"email\":\"a2@test.com\",\"birthDate\":\"2000-01-01\"}"),
			Arguments.of("missing phone", "{\"fullName\":\"A\",\"email\":\"a3@test.com\",\"birthDate\":\"2000-01-01\"}"),
			Arguments.of("blank phone", "{\"fullName\":\"A\",\"phone\":\" \",\"email\":\"a4@test.com\",\"birthDate\":\"2000-01-01\"}"),
			Arguments.of("missing email", "{\"fullName\":\"A\",\"phone\":\"0901111115\",\"birthDate\":\"2000-01-01\"}"),
			Arguments.of("blank email", "{\"fullName\":\"A\",\"phone\":\"0901111116\",\"email\":\" \",\"birthDate\":\"2000-01-01\"}"),
			Arguments.of("missing birthDate", "{\"fullName\":\"A\",\"phone\":\"0901111117\",\"email\":\"a7@test.com\"}"),
			Arguments.of("null birthDate", "{\"fullName\":\"A\",\"phone\":\"0901111118\",\"email\":\"a8@test.com\",\"birthDate\":null}"));
	}

	private static Stream<String> invalidFormattedRequests() {
		return Stream.of(
			validRequest("+84901234567", "invalid-phone-us15@example.com", ""),
			validRequest("0901000019", "not-an-email", ""),
			validRequest("0901000022", "too-long-name@example.com", "")
				.replace("Reception Member", "X".repeat(201)));
	}

	private Account createAccount(AccountRole role, AccountStatus status) {
		UUID id = UUID.randomUUID();
		return createAccount(role, status, "07" + id.toString().replace("-", "").substring(0, 8),
			id + "@actor.test");
	}

	private Account createAccount(AccountRole role, AccountStatus status, String phone, String email) {
		UUID id = UUID.randomUUID();
		return accountRepository.saveAndFlush(new Account(id, role, status, role + " US15 Actor", phone, email,
			LocalDate.of(1990, 1, 1), passwordEncoder.encode(EXISTING_PASSWORD)));
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

	private ResultActions createMember(String token, String body) throws Exception {
		return mockMvc.perform(post(CREATE_MEMBER_PATH)
			.contextPath("/api/v1")
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
			.contentType(MediaType.APPLICATION_JSON)
			.content(body));
	}

	private static String validRequest(String phone, String email, String additionalFields) {
		return """
			{"fullName":"Reception Member","phone":"%s","email":"%s","birthDate":"2000-01-01"%s}
			""".formatted(phone, email, additionalFields);
	}

	private AuditEvent auditFor(UUID accountId) {
		return auditEventRepository.findAll().stream()
			.filter(event -> accountId.equals(event.getTargetId()) && "ACCOUNT_CREATED".equals(event.getAction()))
			.findFirst().orElseThrow();
	}

	private Notification notificationFor(UUID accountId) {
		return notificationRepository.findAll().stream()
			.filter(notification -> accountId.equals(notification.getRecipientAccountId()))
			.findFirst().orElseThrow();
	}

	private void assertCounts(long accounts, long profiles, long audits, long notifications) {
		assertThat(accountRepository.count()).isEqualTo(accounts);
		assertThat(memberProfileRepository.count()).isEqualTo(profiles);
		assertThat(auditEventRepository.count()).isEqualTo(audits);
		assertThat(notificationRepository.count()).isEqualTo(notifications);
	}

	private void installFailureTrigger(String table, String trigger, String function, String message, String sqlState) {
		jdbcTemplate.execute("create function " + function + "() returns trigger language plpgsql as $$ begin "
			+ "raise exception '" + message + "' using errcode = '" + sqlState + "'; end; $$");
		jdbcTemplate.execute("create trigger " + trigger + " before insert on " + table
			+ " for each row execute function " + function + "()");
	}

	private void dropFailureTrigger(String table, String trigger, String function) {
		jdbcTemplate.execute("drop trigger if exists " + trigger + " on " + table);
		jdbcTemplate.execute("drop function if exists " + function + "()");
	}

	private String phoneFor(AccountRole role) {
		return switch (role) {
			case MEMBER -> "0901000081";
			case COACH -> "0901000082";
			case MANAGER -> "0901000083";
			case RECEPTIONIST -> "0901000084";
		};
	}
}
