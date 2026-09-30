package com.scms.backend.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.TimeZone;
import java.util.UUID;

import com.jayway.jsonpath.JsonPath;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
class MemberProfileIntegrationTests {

	private static final String PROFILE_PATH = "/api/v1/members/me/profile";
	private static final String LOGIN_PATH = "/api/v1/auth/login";
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
	private EntityManager entityManager;

	@Autowired
	private Clock clock;

	@Test
	void memberGetsOnlyOwnCompleteProfileAndResponseContainsNoSecrets() throws Exception {
		MemberProfile ownProfile = createMember("Profile Owner", "0901000001", "owner@example.com");
		ownProfile.updateDetails("https://cdn.example/owner.png", "Run a marathon", "Emergency Person",
			"0911000001");
		memberProfileRepository.saveAndFlush(ownProfile);
		createMember("Other Member", "0901000002", "other@example.com");
		Account account = ownProfile.getAccount();

		String body = getProfile(accessToken(account))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.accountId").value(account.getId().toString()))
			.andExpect(jsonPath("$.memberId").value(ownProfile.getMemberCode()))
			.andExpect(jsonPath("$.fullName").value("Profile Owner"))
			.andExpect(jsonPath("$.phone").value("0901000001"))
			.andExpect(jsonPath("$.email").value("owner@example.com"))
			.andExpect(jsonPath("$.birthDate").value("2000-01-15"))
			.andExpect(jsonPath("$.role").value("MEMBER"))
			.andExpect(jsonPath("$.status").value("ACTIVE"))
			.andExpect(jsonPath("$.profileImageUrl").value("https://cdn.example/owner.png"))
			.andExpect(jsonPath("$.fitnessGoal").value("Run a marathon"))
			.andExpect(jsonPath("$.emergencyContactName").value("Emergency Person"))
			.andExpect(jsonPath("$.emergencyContactPhone").value("0911000001"))
			.andExpect(jsonPath("$.password").doesNotExist())
			.andExpect(jsonPath("$.passwordHash").doesNotExist())
			.andExpect(jsonPath("$.accessToken").doesNotExist())
			.andReturn().getResponse().getContentAsString();

		assertThat(body).doesNotContain(PASSWORD).doesNotContain(account.getPasswordHash());
	}

	@Test
	void profileEndpointsRequireAuthenticationAndMemberRole() throws Exception {
		mockMvc.perform(get(PROFILE_PATH).contextPath("/api/v1"))
			.andExpect(status().isUnauthorized());
		getProfile("not-a-jwt").andExpect(status().isUnauthorized());

		for (AccountRole role : new AccountRole[] { AccountRole.COACH, AccountRole.RECEPTIONIST,
				AccountRole.MANAGER }) {
			Account account = createAccount(role, AccountStatus.ACTIVE, nextPhone(role),
				role.name().toLowerCase() + "@example.com");
			getProfile(accessToken(account)).andExpect(status().isForbidden());
			patchProfile(accessToken(account), "{}").andExpect(status().isForbidden());
		}
	}

	@Test
	void patchUpdatesAllowedFieldsAndPreservesImmutableSecurityDataWithoutSideEffects() throws Exception {
		MemberProfile profile = createMember("Old Name", "0902000001", "old@example.com");
		Account account = profile.getAccount();
		String memberId = profile.getMemberCode();
		String passwordHash = account.getPasswordHash();
		AccountRole role = account.getRole();
		AccountStatus status = account.getStatus();
		Long accountVersion = account.getVersion();
		Long profileVersion = profile.getVersion();
		long auditsBefore = auditEventRepository.count();
		long notificationsBefore = notificationRepository.count();

		patchProfile(accessToken(account), """
			{
			  "fullName": "  New Name  ",
			  "phone": " 0902000002 ",
			  "email": " NEW@Example.COM ",
			  "birthDate": "1995-05-20",
			  "profileImageUrl": " https://cdn.example/new.png ",
			  "fitnessGoal": " Build strength ",
			  "emergencyContactName": " Emergency Name ",
			  "emergencyContactPhone": " 0912000002 "
			}
			""")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.fullName").value("New Name"))
			.andExpect(jsonPath("$.phone").value("0902000002"))
			.andExpect(jsonPath("$.email").value("new@example.com"))
			.andExpect(jsonPath("$.birthDate").value("1995-05-20"))
			.andExpect(jsonPath("$.profileImageUrl").value("https://cdn.example/new.png"))
			.andExpect(jsonPath("$.fitnessGoal").value("Build strength"))
			.andExpect(jsonPath("$.emergencyContactName").value("Emergency Name"))
			.andExpect(jsonPath("$.emergencyContactPhone").value("0912000002"))
			.andExpect(jsonPath("$.accountId").value(account.getId().toString()))
			.andExpect(jsonPath("$.memberId").value(memberId));

		entityManager.clear();
		Account updatedAccount = accountRepository.findById(account.getId()).orElseThrow();
		MemberProfile updatedProfile = memberProfileRepository.findById(account.getId()).orElseThrow();
		assertThat(updatedAccount.getRole()).isEqualTo(role);
		assertThat(updatedAccount.getStatus()).isEqualTo(status);
		assertThat(updatedAccount.getPasswordHash()).isEqualTo(passwordHash);
		assertThat(updatedAccount.getVersion()).isGreaterThan(accountVersion);
		assertThat(updatedProfile.getMemberCode()).isEqualTo(memberId);
		assertThat(updatedProfile.getVersion()).isGreaterThan(profileVersion);
		assertThat(auditEventRepository.count()).isEqualTo(auditsBefore);
		assertThat(notificationRepository.count()).isEqualTo(notificationsBefore);
	}

	@Test
	void omittedFieldsStayUnchangedEmptyPatchIsNoOpAndExplicitNullClearsOptionalFields() throws Exception {
		MemberProfile profile = createMember("Stable Name", "0903000001", "stable@example.com");
		profile.updateDetails("https://cdn.example/stable.png", "Stable goal", "Stable Contact", "0913000001");
		memberProfileRepository.saveAndFlush(profile);
		Account account = profile.getAccount();
		String token = accessToken(account);

		patchProfile(token, "{\"fitnessGoal\":\" Updated goal \"}")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.fullName").value("Stable Name"))
			.andExpect(jsonPath("$.phone").value("0903000001"))
			.andExpect(jsonPath("$.profileImageUrl").value("https://cdn.example/stable.png"))
			.andExpect(jsonPath("$.fitnessGoal").value("Updated goal"))
			.andExpect(jsonPath("$.emergencyContactName").value("Stable Contact"));

		String beforeNoOp = getProfile(token).andReturn().getResponse().getContentAsString();
		String afterNoOp = patchProfile(token, "{}")
			.andExpect(status().isOk())
			.andReturn().getResponse().getContentAsString();
		assertThat(afterNoOp).isEqualTo(beforeNoOp);

		patchProfile(token, """
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
	void patchValidatesRequiredFormatsDatesAndOptionalBlankValues() throws Exception {
		Account account = createMember("Validation Member", "0904000001", "validation@example.com").getAccount();
		String token = accessToken(account);

		assertValidation(token, "{\"fullName\":null}", "fullName");
		assertValidation(token, "{\"phone\":null}", "phone");
		assertValidation(token, "{\"email\":null}", "email");
		assertValidation(token, "{\"birthDate\":null}", "birthDate");
		assertValidation(token, "{\"fullName\":\"   \"}", "fullName");
		assertValidation(token, "{\"fullName\":\"%s\"}".formatted("x".repeat(201)), "fullName");
		assertValidation(token, "{\"phone\":\"+84901234567\"}", "phone");
		assertValidation(token, "{\"email\":\"bad-email\"}", "email");
		assertValidation(token, "{\"email\":\"%s@example.com\"}".formatted("x".repeat(310)), "email");
		assertValidation(token, "{\"profileImageUrl\":\"   \"}", "profileImageUrl");
		assertValidation(token, "{\"fitnessGoal\":\"   \"}", "fitnessGoal");
		assertValidation(token, "{\"emergencyContactName\":\"   \"}", "emergencyContactName");
		assertValidation(token, "{\"emergencyContactPhone\":\"123\"}", "emergencyContactPhone");
		assertValidation(token, "{\"birthDate\":\"%s\"}".formatted(LocalDate.now(clock).plusDays(1)),
			"birthDate");
	}

	@Test
	void duplicateIdentifiersConflictWhileInactiveIdentifiersCanBeReused() throws Exception {
		Account member = createMember("Updating Member", "0905000001", "updating@example.com").getAccount();
		createAccount(AccountRole.COACH, AccountStatus.ACTIVE, "0905000002", "active-owner@example.com");
		createAccount(AccountRole.RECEPTIONIST, AccountStatus.INACTIVE, "0905000003", "inactive-owner@example.com");
		String token = accessToken(member);

		patchProfile(token, "{\"email\":\" ACTIVE-OWNER@EXAMPLE.COM \"}")
			.andExpect(status().isConflict())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.code").value("EMAIL_ALREADY_EXISTS"));
		patchProfile(token, "{\"phone\":\"0905000002\"}")
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("PHONE_ALREADY_EXISTS"));

		patchProfile(token, "{\"email\":\"inactive-owner@example.com\",\"phone\":\"0905000003\"}")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.email").value("inactive-owner@example.com"))
			.andExpect(jsonPath("$.phone").value("0905000003"));
	}

	@Test
	void forbiddenAndUnknownPatchFieldsAreRejectedWithoutMutation() throws Exception {
		MemberProfile profile = createMember("Protected Member", "0906000001", "protected@example.com");
		String token = accessToken(profile.getAccount());
		String originalMemberId = profile.getMemberCode();

		for (String field : new String[] { "accountId", "memberId", "role", "status", "password", "unknown" }) {
			patchProfile(token, "{\"%s\":\"attempted-value\"}".formatted(field))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
		}

		getProfile(token)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.fullName").value("Protected Member"))
			.andExpect(jsonPath("$.memberId").value(originalMemberId));
	}

	@Test
	void emergencyContactInvariantIsValidatedAgainstFinalPatchedState() throws Exception {
		Account account = createMember("Emergency Member", "0907000001", "emergency@example.com").getAccount();
		String token = accessToken(account);

		assertValidation(token, "{\"emergencyContactName\":\"Name Only\"}", "emergencyContact");
		patchProfile(token,
			"{\"emergencyContactName\":\"Contact One\",\"emergencyContactPhone\":\"0917000001\"}")
			.andExpect(status().isOk());
		patchProfile(token, "{\"emergencyContactPhone\":\"0917000002\"}")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.emergencyContactName").value("Contact One"))
			.andExpect(jsonPath("$.emergencyContactPhone").value("0917000002"));
		patchProfile(token, "{\"emergencyContactName\":\"Contact Two\"}")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.emergencyContactName").value("Contact Two"));
		assertValidation(token, "{\"emergencyContactName\":null}", "emergencyContact");
		patchProfile(token, "{\"emergencyContactName\":null,\"emergencyContactPhone\":null}")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.emergencyContactName").doesNotExist())
			.andExpect(jsonPath("$.emergencyContactPhone").doesNotExist());
	}

	@Test
	void updatedEmailAndPhoneBecomeLoginIdentifiersAndOldValuesStopWorking() throws Exception {
		Account account = createMember("Login Member", "0908000001", "old-login@example.com").getAccount();
		String token = accessToken(account);

		patchProfile(token, "{\"email\":\"new-login@example.com\",\"phone\":\"0908000002\"}")
			.andExpect(status().isOk());

		performLogin("new-login@example.com").andExpect(status().isOk());
		performLogin("0908000002").andExpect(status().isOk());
		performLogin("old-login@example.com").andExpect(status().isUnauthorized());
		performLogin("0908000001").andExpect(status().isUnauthorized());
	}

	private MemberProfile createMember(String fullName, String phone, String email) {
		UUID id = UUID.randomUUID();
		Account account = accountRepository.saveAndFlush(new Account(id, AccountRole.MEMBER, AccountStatus.ACTIVE,
			fullName, phone, email, LocalDate.of(2000, 1, 15), passwordEncoder.encode(PASSWORD)));
		return memberProfileRepository.saveAndFlush(new MemberProfile(account));
	}

	private Account createAccount(AccountRole role, AccountStatus status, String phone, String email) {
		UUID id = UUID.randomUUID();
		return accountRepository.saveAndFlush(new Account(id, role, status, role.name() + " Account", phone, email,
			LocalDate.of(2000, 1, 15), passwordEncoder.encode(PASSWORD)));
	}

	private String accessToken(Account account) throws Exception {
		MvcResult result = performLogin(account.getEmail()).andExpect(status().isOk()).andReturn();
		return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
	}

	private org.springframework.test.web.servlet.ResultActions performLogin(String identifier) throws Exception {
		return mockMvc.perform(post(LOGIN_PATH)
			.contextPath("/api/v1")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"identifier\":\"%s\",\"password\":\"%s\"}".formatted(identifier, PASSWORD)));
	}

	private org.springframework.test.web.servlet.ResultActions getProfile(String token) throws Exception {
		return mockMvc.perform(get(PROFILE_PATH)
			.contextPath("/api/v1")
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
	}

	private org.springframework.test.web.servlet.ResultActions patchProfile(String token, String body) throws Exception {
		return mockMvc.perform(patch(PROFILE_PATH)
			.contextPath("/api/v1")
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
			.contentType(MediaType.APPLICATION_JSON)
			.content(body));
	}

	private void assertValidation(String token, String body, String field) throws Exception {
		patchProfile(token, body)
			.andExpect(status().isBadRequest())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
			.andExpect(jsonPath("$.errors." + field).exists());
	}

	private String nextPhone(AccountRole role) {
		return switch (role) {
			case COACH -> "0909000001";
			case RECEPTIONIST -> "0909000002";
			case MANAGER -> "0909000003";
			case MEMBER -> "0909000004";
		};
	}
}
