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
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AssignmentDiscoveryController.class)
@AutoConfigureMockMvc
@Import({ SecurityConfiguration.class, SecurityProblemDetailHandler.class })
@TestPropertySource(properties = {
	"scms.cors.allowed-origins=http://localhost:5173",
	"scms.security.jwt-secret=test-only-jwt-secret-at-least-32-bytes-long"
})
class AssignmentDiscoveryControllerTests {

	@Autowired MockMvc mockMvc;
	@MockitoBean AssignmentDiscoveryService service;
	@MockitoBean ActiveAccountJwtValidator activeAccountJwtValidator;

	private final UUID managerId = UUID.randomUUID();

	@Test
	void managerCanPageAndFilterCandidates() throws Exception {
		UUID memberId = UUID.randomUUID();
		var candidate = new AssignmentCandidateResponse(memberId, "MB-100001", "Member One",
			"0900000001", "member@example.test", Instant.parse("2026-11-01T00:00:00Z"),
			AssignmentCandidateStatus.UNASSIGNED);
		when(service.candidates(eq(managerId), eq("member"), eq(AssignmentCandidateStatus.UNASSIGNED),
				any(Pageable.class)))
			.thenReturn(new AssignmentCandidatePageResponse(List.of(candidate), 1, 5, 6, 2));

		mockMvc.perform(get("/api/v1/manager/coach-assignments/candidates")
			.contextPath("/api/v1").with(role("MANAGER"))
			.queryParam("query", "member").queryParam("assignmentStatus", "UNASSIGNED")
			.queryParam("page", "1").queryParam("size", "5"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[0].memberAccountId").value(memberId.toString()))
			.andExpect(jsonPath("$.content[0].assignmentStatus").value("UNASSIGNED"))
			.andExpect(jsonPath("$.page").value(1))
			.andExpect(jsonPath("$.totalElements").value(6));
	}

	@Test
	void managerCanReadCoachWorkloads() throws Exception {
		UUID coachId = UUID.randomUUID();
		when(service.workloads(managerId)).thenReturn(List.of(new CoachAssignmentWorkloadResponse(
			coachId, "Coach One", "0900000002", "coach@example.test", 4, null, true)));

		mockMvc.perform(get("/api/v1/manager/coaches/assignment-workloads")
			.contextPath("/api/v1").with(role("MANAGER")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].coachAccountId").value(coachId.toString()))
			.andExpect(jsonPath("$[0].activeMemberCount").value(4))
			.andExpect(jsonPath("$[0].maximumMemberCount").value(org.hamcrest.Matchers.nullValue()))
			.andExpect(jsonPath("$[0].availableForAssignment").value(true));
	}

	@Test
	void candidatesRejectInvalidPaginationAndStatus() throws Exception {
		for (String parameter : new String[] { "page=-1", "size=0", "size=101" }) {
			String[] pair = parameter.split("=");
			mockMvc.perform(get("/api/v1/manager/coach-assignments/candidates")
				.contextPath("/api/v1").with(role("MANAGER")).queryParam(pair[0], pair[1]))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
		}
		mockMvc.perform(get("/api/v1/manager/coach-assignments/candidates")
			.contextPath("/api/v1").with(role("MANAGER"))
			.queryParam("assignmentStatus", "ACTIVE"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
	}

	@Test
	void discoveryEndpointsRejectUnauthenticatedAndNonManagerRoles() throws Exception {
		for (String path : new String[] {
			"/api/v1/manager/coach-assignments/candidates",
			"/api/v1/manager/coaches/assignment-workloads"
		}) {
			mockMvc.perform(get(path).contextPath("/api/v1"))
				.andExpect(status().isUnauthorized());
			for (String role : new String[] { "MEMBER", "COACH", "RECEPTIONIST" }) {
				mockMvc.perform(get(path).contextPath("/api/v1").with(role(role)))
					.andExpect(status().isForbidden());
			}
		}
	}

	private JwtRequestPostProcessor role(String role) {
		return jwt().jwt(token -> token.subject(managerId.toString()))
			.authorities(new SimpleGrantedAuthority("ROLE_" + role));
	}
}
