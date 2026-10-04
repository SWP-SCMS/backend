package com.scms.backend.manager;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.account.MemberProfile;
import com.scms.backend.account.MemberProfileRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class ManagerAccountIntegrationTests {

	@Container
	@ServiceConnection
	static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

	@Autowired MockMvc mockMvc;
	@Autowired AccountRepository accounts;
	@Autowired MemberProfileRepository memberProfiles;
	@Autowired PasswordEncoder passwords;
	@Autowired JwtEncoder jwtEncoder;

	@Test
	void staffSearchWorksWithOptionalFiltersAndRejectsMemberRole() throws Exception {
		Account manager = account(AccountRole.MANAGER, "Manager");
		account(AccountRole.COACH, "Coach");

		mockMvc.perform(get("/api/v1/manager/staff-accounts").contextPath("/api/v1")
			.header(HttpHeaders.AUTHORIZATION, bearer(manager)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content.length()").value(2));
		mockMvc.perform(get("/api/v1/manager/staff-accounts").contextPath("/api/v1")
			.queryParam("role", "MEMBER").header(HttpHeaders.AUTHORIZATION, bearer(manager)))
			.andExpect(status().isBadRequest());
	}

	@Test
	void suspensionAndCoachDeactivationPersistOnPostgres() throws Exception {
		Account manager = account(AccountRole.MANAGER, "Manager");
		Account member = account(AccountRole.MEMBER, "Member");
		Account coach = account(AccountRole.COACH, "Coach");
		String memberToken = bearer(member);

		mockMvc.perform(patch("/api/v1/manager/members/{id}/status", member.getId()).contextPath("/api/v1")
			.header(HttpHeaders.AUTHORIZATION, bearer(manager)).contentType(MediaType.APPLICATION_JSON)
			.content("{\"status\":\"SUSPENDED\",\"reason\":\"QA\"}"))
			.andExpect(status().isOk()).andExpect(jsonPath("$.status").value("SUSPENDED"));
		mockMvc.perform(get("/api/v1/members/me/profile").contextPath("/api/v1")
			.header(HttpHeaders.AUTHORIZATION, memberToken)).andExpect(status().isUnauthorized());

		mockMvc.perform(patch("/api/v1/manager/staff-accounts/{id}/status", coach.getId()).contextPath("/api/v1")
			.header(HttpHeaders.AUTHORIZATION, bearer(manager)).contentType(MediaType.APPLICATION_JSON)
			.content("{\"status\":\"INACTIVE\",\"reason\":\"QA\"}"))
			.andExpect(status().isOk()).andExpect(jsonPath("$.status").value("INACTIVE"));
	}

	@Test
	void managerListsReadsAndUpdatesMemberAccounts() throws Exception {
		Account manager = account(AccountRole.MANAGER, "Manager");
		Account memberAccount = account(AccountRole.MEMBER, "Member Before");
		MemberProfile member = memberProfiles.saveAndFlush(new MemberProfile(memberAccount));

		mockMvc.perform(get("/api/v1/manager/members").contextPath("/api/v1")
			.queryParam("query", member.getMemberCode()).queryParam("status", "ACTIVE")
			.header(HttpHeaders.AUTHORIZATION, bearer(manager)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content.length()").value(1))
			.andExpect(jsonPath("$.content[0].accountId").value(memberAccount.getId().toString()))
			.andExpect(jsonPath("$.content[0].memberId").value(member.getMemberCode()));

		mockMvc.perform(get("/api/v1/manager/members/{id}", memberAccount.getId()).contextPath("/api/v1")
			.header(HttpHeaders.AUTHORIZATION, bearer(manager)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.fullName").value("Member Before"));

		mockMvc.perform(patch("/api/v1/manager/members/{id}", memberAccount.getId()).contextPath("/api/v1")
			.header(HttpHeaders.AUTHORIZATION, bearer(manager)).contentType(MediaType.APPLICATION_JSON)
			.content("{\"fullName\":\"Member After\",\"fitnessGoal\":\"Improve mobility\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.fullName").value("Member After"))
			.andExpect(jsonPath("$.fitnessGoal").value("Improve mobility"))
			.andExpect(jsonPath("$.status").value("ACTIVE"));

		mockMvc.perform(get("/api/v1/manager/members").contextPath("/api/v1")
			.header(HttpHeaders.AUTHORIZATION, bearer(memberAccount)))
			.andExpect(status().isForbidden());
	}

	private Account account(AccountRole role, String name) {
		UUID id = UUID.randomUUID();
		return accounts.saveAndFlush(new Account(id, role, AccountStatus.ACTIVE, name,
			"09" + id.toString().replace("-", "").substring(0, 8), id + "@example.test",
			LocalDate.of(1990, 1, 1), passwords.encode("password")));
	}

	private String bearer(Account account) {
		Instant now = Instant.now();
		JwtClaimsSet claims = JwtClaimsSet.builder().subject(account.getId().toString()).issuedAt(now)
			.expiresAt(now.plusSeconds(900)).claim("role", account.getRole().name()).build();
		return "Bearer " + jwtEncoder.encode(JwtEncoderParameters.from(
			JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
	}
}
