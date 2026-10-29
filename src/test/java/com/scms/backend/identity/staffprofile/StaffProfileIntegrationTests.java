package com.scms.backend.identity.staffprofile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.audit.AuditEvent;
import com.scms.backend.audit.AuditEventRepository;
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
class StaffProfileIntegrationTests {

	private static final String PATH = "/api/v1/staff/me/profile";
	private static final AtomicInteger IDENTIFIER_SEQUENCE = new AtomicInteger(300_000_000);

	@Container
	@ServiceConnection
	static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

	@Autowired MockMvc mockMvc;
	@Autowired AccountRepository accounts;
	@Autowired AuditEventRepository audits;
	@Autowired PasswordEncoder passwordEncoder;
	@Autowired JwtEncoder jwtEncoder;
	@Autowired JdbcTemplate jdbc;
	@Autowired EntityManager entityManager;

	@ParameterizedTest
	@EnumSource(value = AccountRole.class, names = {"MANAGER", "RECEPTIONIST", "COACH"})
	void activeStaffUpdateCreatesOneSafeAuditWithActorSnapshot(AccountRole role) throws Exception {
		Account actor = createAccount(role);
		long auditCount = audits.count();
		String updatedPhone = "091000000" + role.ordinal();

		patch(actor, """
			{"fullName":"Updated Staff","phone":"%s","birthDate":"1991-02-03"}
			""".formatted(updatedPhone))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.accountId").value(actor.getId().toString()))
			.andExpect(jsonPath("$.role").value(role.name()))
			.andExpect(jsonPath("$.status").value("ACTIVE"))
			.andExpect(jsonPath("$.password").doesNotExist())
			.andExpect(jsonPath("$.passwordHash").doesNotExist());

		assertThat(audits.count()).isEqualTo(auditCount + 1);
		AuditEvent event = audits.findAll().stream()
			.filter(candidate -> "STAFF_SELF_PROFILE_UPDATED".equals(candidate.getAction()))
			.filter(candidate -> actor.getId().equals(candidate.getTargetId()))
			.findFirst().orElseThrow();
		assertThat(event.getActorAccountId()).isEqualTo(actor.getId());
		assertThat(event.getActorNameSnapshot()).isEqualTo("Updated Staff");
		assertThat(event.getActorRoleSnapshot()).isEqualTo(role);
		assertThat(event.getBeforeData()).containsKeys("fullName", "phone", "email", "birthDate")
			.doesNotContainKeys("password", "passwordHash", "role", "status");
		assertThat(event.getBeforeData().get("email").toString()).doesNotContain(actor.getEmail());
		assertThat(event.getAfterData().get("phone").toString()).doesNotContain(updatedPhone);
	}

	@Test
	void identicalPatchCreatesNoAudit() throws Exception {
		Account actor = createAccount(AccountRole.COACH);
		long auditCount = audits.count();

		patch(actor, "{}").andExpect(status().isOk());

		assertThat(audits.count()).isEqualTo(auditCount);
	}

	@ParameterizedTest
	@MethodSource("readOnlyFields")
	void patchRejectsReadOnlyAndUnknownFields(String field) throws Exception {
		patch(createAccount(AccountRole.MANAGER), "{\"" + field + "\":\"attempted-value\"}")
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
	}

	@Test
	void directEmailChangeRequiresVerificationAndDuplicatePhoneKeepsSpecificConflict() throws Exception {
		Account actor = createAccount(AccountRole.RECEPTIONIST);
		Account existing = createAccount(AccountRole.COACH);

		patch(actor, "{\"email\":\"" + existing.getEmail() + "\"}")
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("EMAIL_CHANGE_VERIFICATION_REQUIRED"));
		patch(actor, "{\"phone\":\"" + existing.getPhone() + "\"}")
			.andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("PHONE_ALREADY_EXISTS"));
	}

	@Test
	void databaseUniqueRaceReturnsConflictAndRollsBackAccountAndAudit() throws Exception {
		Account actor = createAccount(AccountRole.COACH);
		String oldPhone = actor.getPhone();
		long auditCount = audits.count();
		installFailureTrigger();
		try {
			patch(actor, "{\"phone\":\"0999999999\"}")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("ACCOUNT_IDENTIFIER_ALREADY_EXISTS"));
		}
		finally {
			dropFailureTrigger();
		}

		entityManager.clear();
		assertThat(accounts.findById(actor.getId()).orElseThrow().getPhone()).isEqualTo(oldPhone);
		assertThat(audits.count()).isEqualTo(auditCount);
	}

	private ResultActions patch(Account actor, String body) throws Exception {
		return mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(PATH)
			.contextPath("/api/v1")
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken(actor))
			.contentType(MediaType.APPLICATION_JSON).content(body));
	}

	private Account createAccount(AccountRole role) {
		int sequence = IDENTIFIER_SEQUENCE.getAndIncrement();
		return accounts.saveAndFlush(new Account(UUID.randomUUID(), role, AccountStatus.ACTIVE, role + " Staff",
			"0" + sequence, "staff-" + sequence + "@example.com", LocalDate.of(1990, 1, 1),
			passwordEncoder.encode("Password123")));
	}

	private String accessToken(Account account) {
		Instant now = Instant.now();
		JwtClaimsSet claims = JwtClaimsSet.builder().subject(account.getId().toString()).issuedAt(now)
			.expiresAt(now.plusSeconds(900)).claim("role", account.getRole().name()).build();
		return jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
			.getTokenValue();
	}

	private void installFailureTrigger() {
		jdbc.execute("create function staff_profile_identifier_race() returns trigger language plpgsql as $$ begin "
			+ "raise exception 'duplicate key value violates unknown identifier constraint' using errcode = '23505'; end; $$");
		jdbc.execute("create trigger staff_profile_identifier_race before update on accounts "
			+ "for each row execute function staff_profile_identifier_race()");
	}

	private void dropFailureTrigger() {
		jdbc.execute("drop trigger if exists staff_profile_identifier_race on accounts");
		jdbc.execute("drop function if exists staff_profile_identifier_race()");
	}

	private static Stream<String> readOnlyFields() {
		return Stream.of("id", "role", "status", "password", "passwordHash", "profileImageUrl", "unknown");
	}
}
