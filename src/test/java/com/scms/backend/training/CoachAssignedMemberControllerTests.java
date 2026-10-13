package com.scms.backend.training;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.scms.backend.account.AccountStatus;
import com.scms.backend.auth.ActiveAccountJwtValidator;
import com.scms.backend.common.error.SecurityProblemDetailHandler;
import com.scms.backend.config.SecurityConfiguration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = CoachAssignedMemberController.class)
@AutoConfigureMockMvc
@Import({ SecurityConfiguration.class, SecurityProblemDetailHandler.class })
@TestPropertySource(properties = {
	"scms.cors.allowed-origins=http://localhost:5173",
	"scms.security.jwt-secret=test-only-jwt-secret-at-least-32-bytes-long"
})
class CoachAssignedMemberControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private CoachAssignedMemberService service;

	@MockitoBean
	private ActiveAccountJwtValidator activeAccountJwtValidator;

	private static final UUID COACH_ID = UUID.randomUUID();
	private static final UUID MEMBER_ID = UUID.randomUUID();

	// ── US40 list ─────────────────────────────────────────────────────────────

	@Test
	void listAssignedMembers_returns200_whenCoach() throws Exception {
		var summary = new CoachAssignedMemberSummaryResponse(
			MEMBER_ID, "MBR-000001", "Test Member", AccountStatus.ACTIVE,
			null, "Lose weight", UUID.randomUUID(), Instant.now());
		var page = new CoachAssignedMemberPageResponse(List.of(summary), 0, 20, 1L, 1);

		when(service.listAssignedMembers(eq(COACH_ID), any(Pageable.class))).thenReturn(page);

		mockMvc.perform(get("/coach/members")
				.with(jwt().jwt(b -> b.subject(COACH_ID.toString()))
						.authorities(new SimpleGrantedAuthority("ROLE_COACH"))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.totalElements").value(1))
			.andExpect(jsonPath("$.content[0].memberId").value("MBR-000001"));
	}

	@Test
	void listAssignedMembers_returns401_whenUnauthenticated() throws Exception {
		mockMvc.perform(get("/coach/members"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void listAssignedMembers_returns403_whenMember() throws Exception {
		mockMvc.perform(get("/coach/members")
				.with(jwt().jwt(b -> b.subject(UUID.randomUUID().toString()))
						.authorities(new SimpleGrantedAuthority("ROLE_MEMBER"))))
			.andExpect(status().isForbidden());
	}

	@Test
	void listAssignedMembers_returns403_whenManager() throws Exception {
		mockMvc.perform(get("/coach/members")
				.with(jwt().jwt(b -> b.subject(UUID.randomUUID().toString()))
						.authorities(new SimpleGrantedAuthority("ROLE_MANAGER"))))
			.andExpect(status().isForbidden());
	}

	@Test
	void listAssignedMembers_returns400_whenBadPagination() throws Exception {
		mockMvc.perform(get("/coach/members?page=-1&size=200")
				.with(jwt().jwt(b -> b.subject(COACH_ID.toString()))
						.authorities(new SimpleGrantedAuthority("ROLE_COACH"))))
			.andExpect(status().isBadRequest());
	}

	// US41 detail

	@Test
	void getMemberTrainingProfile_returns200_whenActiveAssignment() throws Exception {
		var profile = new CoachMemberTrainingProfileResponse(
			MEMBER_ID, "MBR-000001", "Test Member",
			"0901234567", "member@test.com",
			java.time.LocalDate.of(1990, 1, 1), AccountStatus.ACTIVE,
			null, "Build muscle", null, null,
			UUID.randomUUID(), Instant.now());

		when(service.getMemberTrainingProfile(eq(COACH_ID), eq(MEMBER_ID))).thenReturn(profile);

		mockMvc.perform(get("/coach/members/" + MEMBER_ID)
				.with(jwt().jwt(b -> b.subject(COACH_ID.toString()))
						.authorities(new SimpleGrantedAuthority("ROLE_COACH"))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.memberId").value("MBR-000001"))
			.andExpect(jsonPath("$.fullName").value("Test Member"))
			.andExpect(jsonPath("$.fitnessGoal").value("Build muscle"));
	}

	@Test
	void getMemberTrainingProfile_returns401_whenUnauthenticated() throws Exception {
		mockMvc.perform(get("/coach/members/" + MEMBER_ID))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void getMemberTrainingProfile_returns403_whenReceptionist() throws Exception {
		mockMvc.perform(get("/coach/members/" + MEMBER_ID)
				.with(jwt().jwt(b -> b.subject(UUID.randomUUID().toString()))
						.authorities(new SimpleGrantedAuthority("ROLE_RECEPTIONIST"))))
			.andExpect(status().isForbidden());
	}

}
