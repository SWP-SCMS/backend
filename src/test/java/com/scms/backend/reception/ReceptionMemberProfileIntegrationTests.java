package com.scms.backend.reception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import java.util.UUID;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.account.MemberProfile;
import com.scms.backend.account.MemberProfileRepository;
import com.scms.backend.audit.AuditEventRepository;
import com.scms.backend.notification.NotificationRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class ReceptionMemberProfileIntegrationTests {

	private static final String SEARCH_PATH = "/api/v1/reception/members/search";
	private static final String PASSWORD = "Password123";
	private static final AtomicInteger IDENTIFIER_SEQUENCE = new AtomicInteger(100_000_000);

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

	@Test
	void activeReceptionistSearchesActiveMemberByTrimmedMemberIdWithoutExposingSecrets() throws Exception {
		Account receptionist = createAccount(AccountRole.RECEPTIONIST, AccountStatus.ACTIVE);
		MemberProfile member = createMember(AccountStatus.ACTIVE, "Nguyen Van A", "0901111111",
			"member-search@example.com");

		String body = searchByMemberId(accessToken(receptionist), "  " + member.getMemberCode() + "  ")
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.accountId").value(member.getAccountId().toString()))
			.andExpect(jsonPath("$.memberId").value(member.getMemberCode()))
			.andExpect(jsonPath("$.fullName").value("Nguyen Van A"))
			.andExpect(jsonPath("$.phone").value("0901111111"))
			.andExpect(jsonPath("$.email").value("member-search@example.com"))
			.andExpect(jsonPath("$.birthDate").value("2000-01-01"))
			.andExpect(jsonPath("$.status").value("ACTIVE"))
			.andExpect(jsonPath("$.password").doesNotExist())
			.andExpect(jsonPath("$.passwordHash").doesNotExist())
			.andExpect(jsonPath("$.accessToken").doesNotExist())
			.andExpect(jsonPath("$.refreshToken").doesNotExist())
			.andReturn().getResponse().getContentAsString();

		assertThat(body).doesNotContain(PASSWORD).doesNotContain(member.getAccount().getPasswordHash());
	}

	@Test
	void receptionistSearchesSuspendedMemberByTrimmedPhone() throws Exception {
		Account receptionist = createAccount(AccountRole.RECEPTIONIST, AccountStatus.ACTIVE);
		MemberProfile member = createMember(AccountStatus.SUSPENDED, "Suspended Member", "0901111112",
			"suspended-search@example.com");

		searchByPhone(accessToken(receptionist), " 0901111112 ")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.memberId").value(member.getMemberCode()))
			.andExpect(jsonPath("$.status").value("SUSPENDED"));
	}

	@Test
	void searchHidesInactiveMembersAndUsesNotFoundForMissingMembers() throws Exception {
		Account receptionist = createAccount(AccountRole.RECEPTIONIST, AccountStatus.ACTIVE);
		MemberProfile inactive = createMember(AccountStatus.ACTIVE, "Inactive Member", "0901111113",
			"inactive-search@example.com");
		String token = accessToken(receptionist);

		dropAccountRoleLifecycleConstraint();
		jdbcTemplate.update("update accounts set status = 'INACTIVE' where id = ?", inactive.getAccountId());
		entityManager.clear();
		try {
			searchByMemberId(token, inactive.getMemberCode())
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("MEMBER_NOT_FOUND"));
			searchByPhone(token, "0901111113")
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("MEMBER_NOT_FOUND"));
			getProfile(token, inactive.getMemberCode())
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("MEMBER_NOT_FOUND"));
			patchProfile(token, inactive.getMemberCode(), "{}")
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("MEMBER_NOT_FOUND"));
		}
		finally {
			jdbcTemplate.update("update accounts set status = 'ACTIVE' where id = ?", inactive.getAccountId());
			restoreAccountRoleLifecycleConstraint();
		}
		searchByMemberId(token, "MB-999999999")
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("MEMBER_NOT_FOUND"));

		Account coach = createAccount(AccountRole.COACH, AccountStatus.ACTIVE);
		MemberProfile nonMemberProfile = memberProfileRepository.saveAndFlush(new MemberProfile(coach));
		searchByMemberId(token, nonMemberProfile.getMemberCode())
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("MEMBER_NOT_FOUND"));
		getProfile(token, nonMemberProfile.getMemberCode())
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("MEMBER_NOT_FOUND"));
	}

	@Test
	void searchRequiresExactlyOneValidCriterion() throws Exception {
		String token = accessToken(createAccount(AccountRole.RECEPTIONIST, AccountStatus.ACTIVE));

		search(token)
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
		search(token, "memberId", "MB-100001", "phone", "0901111114")
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
		searchByPhone(token, "+84901111114")
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
			.andExpect(jsonPath("$.errors.phone").exists());
		searchByMemberId(token, "   ")
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
	}

	@Test
	void receptionistGetsCompleteProfilesForActiveAndSuspendedMembers() throws Exception {
		String token = accessToken(createAccount(AccountRole.RECEPTIONIST, AccountStatus.ACTIVE));
		MemberProfile active = createMember(AccountStatus.ACTIVE, "Active Profile", "0901111120",
			"active-profile@example.com");
		MemberProfile suspended = createMember(AccountStatus.SUSPENDED, "Suspended Profile", "0901111121",
			"suspended-profile@example.com");
		active.updateDetails("https://cdn.example/active.png", "Build strength", "Active Contact", "0911111120");
		memberProfileRepository.saveAndFlush(active);

		getProfile(token, active.getMemberCode())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.role").value("MEMBER"))
			.andExpect(jsonPath("$.status").value("ACTIVE"))
			.andExpect(jsonPath("$.fitnessGoal").value("Build strength"))
			.andExpect(jsonPath("$.emergencyContactPhone").value("0911111120"));
		getProfile(token, suspended.getMemberCode())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("SUSPENDED"));
	}

	@Test
	void patchUpdatesEveryEditableFieldWithoutAuditOrNotification() throws Exception {
		String token = accessToken(createAccount(AccountRole.RECEPTIONIST, AccountStatus.ACTIVE));
		MemberProfile member = createMember(AccountStatus.ACTIVE, "Old Name", "0901111130",
			"old-profile@example.com");
		Account account = member.getAccount();
		String passwordHash = account.getPasswordHash();
		long auditsBefore = auditEventRepository.count();
		long notificationsBefore = notificationRepository.count();

		String body = patchProfile(token, member.getMemberCode(), """
			{
			  "fullName":"  New Name  ",
			  "phone":" 0901111131 ",
			  "email":" NEW-PROFILE@Example.COM ",
			  "birthDate":"1995-05-20",
			  "profileImageUrl":" https://cdn.example/new.png ",
			  "fitnessGoal":" Run a marathon ",
			  "emergencyContactName":" Emergency Person ",
			  "emergencyContactPhone":" 0911111131 "
			}
			""")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.accountId").value(account.getId().toString()))
			.andExpect(jsonPath("$.memberId").value(member.getMemberCode()))
			.andExpect(jsonPath("$.fullName").value("New Name"))
			.andExpect(jsonPath("$.phone").value("0901111131"))
			.andExpect(jsonPath("$.email").value("new-profile@example.com"))
			.andExpect(jsonPath("$.birthDate").value("1995-05-20"))
			.andExpect(jsonPath("$.profileImageUrl").value("https://cdn.example/new.png"))
			.andExpect(jsonPath("$.fitnessGoal").value("Run a marathon"))
			.andExpect(jsonPath("$.emergencyContactName").value("Emergency Person"))
			.andExpect(jsonPath("$.emergencyContactPhone").value("0911111131"))
			.andExpect(jsonPath("$.role").value("MEMBER"))
			.andExpect(jsonPath("$.status").value("ACTIVE"))
			.andExpect(jsonPath("$.passwordHash").doesNotExist())
			.andReturn().getResponse().getContentAsString();

		entityManager.clear();
		Account updated = accountRepository.findById(account.getId()).orElseThrow();
		assertThat(updated.getPasswordHash()).isEqualTo(passwordHash);
		assertThat(updated.getRole()).isEqualTo(AccountRole.MEMBER);
		assertThat(updated.getStatus()).isEqualTo(AccountStatus.ACTIVE);
		assertThat(body).doesNotContain(passwordHash).doesNotContain(PASSWORD);
		assertThat(auditEventRepository.count()).isEqualTo(auditsBefore);
		assertThat(notificationRepository.count()).isEqualTo(notificationsBefore);
	}

	@Test
	void patchSuspendedMemberPreservesStatusAndImmutableIdentifiers() throws Exception {
		String token = accessToken(createAccount(AccountRole.RECEPTIONIST, AccountStatus.ACTIVE));
		MemberProfile member = createMember(AccountStatus.SUSPENDED, "Suspended Old", "0901111140",
			"suspended-old@example.com");
		UUID accountId = member.getAccountId();
		String memberId = member.getMemberCode();

		patchProfile(token, memberId, "{\"fullName\":\"Suspended New\"}")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.fullName").value("Suspended New"))
			.andExpect(jsonPath("$.status").value("SUSPENDED"));

		entityManager.clear();
		Account updated = accountRepository.findById(accountId).orElseThrow();
		MemberProfile updatedProfile = memberProfileRepository.findById(accountId).orElseThrow();
		assertThat(updated.getStatus()).isEqualTo(AccountStatus.SUSPENDED);
		assertThat(updated.getId()).isEqualTo(accountId);
		assertThat(updatedProfile.getMemberCode()).isEqualTo(memberId);
	}

	@Test
	void patchSupportsOmitExplicitNullAndEmptyNoOpSemantics() throws Exception {
		String token = accessToken(createAccount(AccountRole.RECEPTIONIST, AccountStatus.ACTIVE));
		MemberProfile member = createMember(AccountStatus.ACTIVE, "Stable Member", "0901111150",
			"stable-reception@example.com");
		member.updateDetails("https://cdn.example/stable.png", "Stable goal", "Stable Contact", "0911111150");
		memberProfileRepository.saveAndFlush(member);

		patchProfile(token, member.getMemberCode(), "{\"fitnessGoal\":\" Updated goal \"}")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.fullName").value("Stable Member"))
			.andExpect(jsonPath("$.profileImageUrl").value("https://cdn.example/stable.png"))
			.andExpect(jsonPath("$.fitnessGoal").value("Updated goal"));

		String before = getProfile(token, member.getMemberCode()).andReturn().getResponse().getContentAsString();
		String after = patchProfile(token, member.getMemberCode(), "{}")
			.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		assertThat(after).isEqualTo(before);

		patchProfile(token, member.getMemberCode(), """
			{"profileImageUrl":null,"fitnessGoal":null,
			 "emergencyContactName":null,"emergencyContactPhone":null}
			""")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.profileImageUrl").doesNotExist())
			.andExpect(jsonPath("$.fitnessGoal").doesNotExist())
			.andExpect(jsonPath("$.emergencyContactName").doesNotExist())
			.andExpect(jsonPath("$.emergencyContactPhone").doesNotExist());
	}

	@Test
	void patchValidatesRequiredOptionalAndEmergencyFinalState() throws Exception {
		String token = accessToken(createAccount(AccountRole.RECEPTIONIST, AccountStatus.ACTIVE));
		MemberProfile member = createMember(AccountStatus.ACTIVE, "Validation Member", "0901111160",
			"validation-reception@example.com");
		String memberId = member.getMemberCode();

		assertValidation(token, memberId, "{\"fullName\":null}", "fullName");
		assertValidation(token, memberId, "{\"phone\":null}", "phone");
		assertValidation(token, memberId, "{\"email\":null}", "email");
		assertValidation(token, memberId, "{\"birthDate\":null}", "birthDate");
		assertValidation(token, memberId, "{\"profileImageUrl\":\"   \"}", "profileImageUrl");
		assertValidation(token, memberId, "{\"emergencyContactName\":\"Name only\"}", "emergencyContact");
		assertValidation(token, memberId,
			"{\"birthDate\":\"" + LocalDate.now().plusDays(1) + "\"}", "birthDate");

		patchProfile(token, memberId,
			"{\"emergencyContactName\":\"Contact\",\"emergencyContactPhone\":\"0911111160\"}")
			.andExpect(status().isOk());
		patchProfile(token, memberId, "{\"emergencyContactPhone\":\"0911111161\"}")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.emergencyContactName").value("Contact"));
		assertValidation(token, memberId, "{\"emergencyContactName\":null}", "emergencyContact");
	}

	@ParameterizedTest
	@MethodSource("forbiddenPatchFields")
	void patchRejectsReadOnlyUnknownAndPasswordFields(String field) throws Exception {
		String token = accessToken(createAccount(AccountRole.RECEPTIONIST, AccountStatus.ACTIVE));
		MemberProfile member = createMember(AccountStatus.ACTIVE, "Protected Member", nextPhone(), nextEmail());

		patchProfile(token, member.getMemberCode(), "{\"" + field + "\":\"attempted-value\"}")
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
	}

	@Test
	void duplicateIdentifiersAndDatabaseRaceReturnSpecificConflictCodes() throws Exception {
		String token = accessToken(createAccount(AccountRole.RECEPTIONIST, AccountStatus.ACTIVE));
		MemberProfile member = createMember(AccountStatus.ACTIVE, "Updating Member", "0901111170",
			"updating-reception@example.com");
		createAccount(AccountRole.COACH, AccountStatus.ACTIVE, "0901111171", "existing-reception@example.com");

		patchProfile(token, member.getMemberCode(), "{\"email\":\" EXISTING-RECEPTION@EXAMPLE.COM \"}")
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("EMAIL_ALREADY_EXISTS"));
		patchProfile(token, member.getMemberCode(), "{\"phone\":\"0901111171\"}")
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("PHONE_ALREADY_EXISTS"));

		installFailureTrigger("accounts", "us11_identifier_race", "us11_force_identifier_race",
			"duplicate key value violates unique constraint \\\"unknown_identifier_constraint\\\"", "23505");
		try {
			patchProfile(token, member.getMemberCode(), "{\"email\":\"race-us11@example.com\"}")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("ACCOUNT_IDENTIFIER_ALREADY_EXISTS"));
		}
		finally {
			dropFailureTrigger("accounts", "us11_identifier_race", "us11_force_identifier_race");
		}
	}

	@ParameterizedTest
	@EnumSource(value = AccountRole.class, names = {"MEMBER", "COACH", "MANAGER"})
	void nonReceptionistRolesAreForbidden(AccountRole role) throws Exception {
		Account actor = createAccount(role, AccountStatus.ACTIVE);
		MemberProfile member = createMember(AccountStatus.ACTIVE, "Forbidden Target", nextPhone(), nextEmail());
		String token = accessToken(actor);

		searchByMemberId(token, member.getMemberCode()).andExpect(status().isForbidden());
		getProfile(token, member.getMemberCode()).andExpect(status().isForbidden());
		patchProfile(token, member.getMemberCode(), "{}").andExpect(status().isForbidden());
	}

	@Test
	void missingInvalidExpiredAndInactiveReceptionistTokensAreUnauthorized() throws Exception {
		MemberProfile member = createMember(AccountStatus.ACTIVE, "Auth Target", nextPhone(), nextEmail());

		searchByMemberId(null, member.getMemberCode()).andExpect(status().isUnauthorized());
		searchByMemberId("not-a-jwt", member.getMemberCode()).andExpect(status().isUnauthorized());

		Account receptionist = createAccount(AccountRole.RECEPTIONIST, AccountStatus.ACTIVE);
		searchByMemberId(accessToken(receptionist, Instant.now().minusSeconds(60)), member.getMemberCode())
			.andExpect(status().isUnauthorized());

		String token = accessToken(receptionist);
		jdbcTemplate.update("update accounts set status = 'INACTIVE' where id = ?", receptionist.getId());
		entityManager.clear();
		searchByMemberId(token, member.getMemberCode()).andExpect(status().isUnauthorized());
	}

	@Test
	void accountAndProfileChangesRollBackTogetherWhenProfilePersistenceFails() throws Exception {
		String token = accessToken(createAccount(AccountRole.RECEPTIONIST, AccountStatus.ACTIVE));
		MemberProfile member = createMember(AccountStatus.ACTIVE, "Atomic Member", "0901111180",
			"atomic-old@example.com");
		installFailureTrigger("member_profiles", "us11_profile_failure", "us11_force_profile_failure",
			"forced profile update failure", "P0001");
		try {
			patchProfile(token, member.getMemberCode(),
				"{\"email\":\"atomic-new@example.com\",\"fitnessGoal\":\"New goal\"}")
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.code").value("INTERNAL_ERROR"));
		}
		finally {
			dropFailureTrigger("member_profiles", "us11_profile_failure", "us11_force_profile_failure");
		}

		entityManager.clear();
		Account unchangedAccount = accountRepository.findById(member.getAccountId()).orElseThrow();
		MemberProfile unchangedProfile = memberProfileRepository.findById(member.getAccountId()).orElseThrow();
		assertThat(unchangedAccount.getEmail()).isEqualTo("atomic-old@example.com");
		assertThat(unchangedProfile.getFitnessGoal()).isNull();
	}

	private static Stream<String> forbiddenPatchFields() {
		return Stream.of("accountId", "memberId", "role", "status", "password", "passwordHash", "version",
			"unknown");
	}

	private MemberProfile createMember(AccountStatus status, String fullName, String phone, String email) {
		Account account = createAccount(AccountRole.MEMBER, status, phone, email, fullName);
		return memberProfileRepository.saveAndFlush(new MemberProfile(account));
	}

	private Account createAccount(AccountRole role, AccountStatus status) {
		return createAccount(role, status, nextPhone(), nextEmail(), role + " Actor");
	}

	private Account createAccount(AccountRole role, AccountStatus status, String phone, String email) {
		return createAccount(role, status, phone, email, role + " Account");
	}

	private Account createAccount(AccountRole role, AccountStatus status, String phone, String email,
			String fullName) {
		UUID id = UUID.randomUUID();
		return accountRepository.saveAndFlush(new Account(id, role, status, fullName, phone, email,
			LocalDate.of(2000, 1, 1), passwordEncoder.encode(PASSWORD)));
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

	private ResultActions searchByMemberId(String token, String memberId) throws Exception {
		return search(token, "memberId", memberId);
	}

	private ResultActions searchByPhone(String token, String phone) throws Exception {
		return search(token, "phone", phone);
	}

	private ResultActions search(String token, String... parameters) throws Exception {
		var request = get(SEARCH_PATH).contextPath("/api/v1");
		if (token != null) {
			request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
		}
		for (int index = 0; index < parameters.length; index += 2) {
			request.queryParam(parameters[index], parameters[index + 1]);
		}
		return mockMvc.perform(request);
	}

	private ResultActions getProfile(String token, String memberId) throws Exception {
		return mockMvc.perform(get(profilePath(memberId))
			.contextPath("/api/v1")
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
	}

	private ResultActions patchProfile(String token, String memberId, String body) throws Exception {
		return mockMvc.perform(patch(profilePath(memberId))
			.contextPath("/api/v1")
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
			.contentType(MediaType.APPLICATION_JSON)
			.content(body));
	}

	private String profilePath(String memberId) {
		return "/api/v1/reception/members/" + memberId + "/profile";
	}

	private void assertValidation(String token, String memberId, String body, String field) throws Exception {
		patchProfile(token, memberId, body)
			.andExpect(status().isBadRequest())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
			.andExpect(jsonPath("$.errors." + field).exists());
	}

	private void installFailureTrigger(String table, String trigger, String function, String message,
			String sqlState) {
		jdbcTemplate.execute("create function " + function + "() returns trigger language plpgsql as $$ begin "
			+ "raise exception '" + message + "' using errcode = '" + sqlState + "'; end; $$");
		jdbcTemplate.execute("create trigger " + trigger + " before update on " + table
			+ " for each row execute function " + function + "()");
	}

	private void dropFailureTrigger(String table, String trigger, String function) {
		jdbcTemplate.execute("drop trigger if exists " + trigger + " on " + table);
		jdbcTemplate.execute("drop function if exists " + function + "()");
	}

	private void dropAccountRoleLifecycleConstraint() {
		jdbcTemplate.execute("alter table accounts drop constraint chk_accounts_role_lifecycle");
	}

	private void restoreAccountRoleLifecycleConstraint() {
		jdbcTemplate.execute("""
			alter table accounts add constraint chk_accounts_role_lifecycle check (
			  (role = 'MEMBER' and status in ('ACTIVE', 'SUSPENDED'))
			  or (role in ('COACH', 'RECEPTIONIST', 'MANAGER') and status in ('ACTIVE', 'INACTIVE'))
			)
			""");
	}

	private static String nextPhone() {
		return "0" + IDENTIFIER_SEQUENCE.getAndIncrement();
	}

	private static String nextEmail() {
		return "us11-" + UUID.randomUUID() + "@example.com";
	}
}
