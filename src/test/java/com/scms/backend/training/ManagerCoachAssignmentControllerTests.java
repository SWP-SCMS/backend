package com.scms.backend.training;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;

import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import com.scms.backend.config.SecurityConfiguration;
import com.scms.backend.common.error.SecurityProblemDetailHandler;
import com.scms.backend.auth.ActiveAccountJwtValidator;

@WebMvcTest(controllers = ManagerCoachAssignmentController.class)
@AutoConfigureMockMvc
@Import({ SecurityConfiguration.class, SecurityProblemDetailHandler.class })
@TestPropertySource(properties = {
	"scms.cors.allowed-origins=http://localhost:5173",
	"scms.security.jwt-secret=test-only-jwt-secret-at-least-32-bytes-long"
})
class ManagerCoachAssignmentControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private CoachAssignmentService service;

	@MockitoBean
	private ActiveAccountJwtValidator activeAccountJwtValidator;

	private final UUID managerId = UUID.randomUUID();
	private final UUID memberId = UUID.randomUUID();
	private final UUID coachId = UUID.randomUUID();
	private final UUID assignmentId = UUID.randomUUID();
	private final Instant now = Instant.parse("2026-10-12T12:00:00Z");

	private JwtRequestPostProcessor managerJwt() {
		return jwt().jwt(jwt -> jwt.subject(managerId.toString()))
				.authorities(new SimpleGrantedAuthority("ROLE_MANAGER"));
	}

	private CoachAssignmentResponse sampleResponse(CoachAssignmentStatus status) {
		return new CoachAssignmentResponse(
				assignmentId,
				memberId,
				coachId,
				"Coach Alpha",
				"0911000003",
				"coach@example.com",
				managerId,
				status,
				now,
				status == CoachAssignmentStatus.ENDED ? now.plusSeconds(3600) : null,
				status == CoachAssignmentStatus.ENDED ? "Program finished" : null,
				now
		);
	}

	@Test
	void assign_whenValidRequest_returns201Created() throws Exception {
		CoachAssignmentResponse response = sampleResponse(CoachAssignmentStatus.ACTIVE);
		when(service.assign(eq(managerId), eq(memberId), any(CoachAssignmentAssignRequest.class)))
				.thenReturn(response);

		mockMvc.perform(post("/api/v1/manager/members/{memberId}/coach-assignment", memberId)
				.contextPath("/api/v1")
				.with(managerJwt())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"coachAccountId\":\"" + coachId + "\",\"reason\":\"New personal program\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(assignmentId.toString()))
				.andExpect(jsonPath("$.memberAccountId").value(memberId.toString()))
				.andExpect(jsonPath("$.coachAccountId").value(coachId.toString()))
				.andExpect(jsonPath("$.coachFullName").value("Coach Alpha"))
				.andExpect(jsonPath("$.status").value("ACTIVE"));
	}

	@Test
	void assign_whenMissingCoachAccountId_returns400BadRequest() throws Exception {
		mockMvc.perform(post("/api/v1/manager/members/{memberId}/coach-assignment", memberId)
				.contextPath("/api/v1")
				.with(managerJwt())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"reason\":\"Missing coach\"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void assign_whenConflict_returns409Conflict() throws Exception {
		when(service.assign(eq(managerId), eq(memberId), any(CoachAssignmentAssignRequest.class)))
				.thenThrow(CoachAssignmentException.plusMembershipRequired());

		mockMvc.perform(post("/api/v1/manager/members/{memberId}/coach-assignment", memberId)
				.contextPath("/api/v1")
				.with(managerJwt())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"coachAccountId\":\"" + coachId + "\"}"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("PLUS_MEMBERSHIP_REQUIRED"));
	}

	@Test
	void getActive_whenExists_returns200Ok() throws Exception {
		CoachAssignmentResponse response = sampleResponse(CoachAssignmentStatus.ACTIVE);
		when(service.getActive(managerId, memberId)).thenReturn(Optional.of(response));

		mockMvc.perform(get("/api/v1/manager/members/{memberId}/coach-assignment", memberId)
				.contextPath("/api/v1")
				.with(managerJwt()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(assignmentId.toString()))
				.andExpect(jsonPath("$.status").value("ACTIVE"));
	}

	@Test
	void getActive_whenNotExists_returns204NoContent() throws Exception {
		when(service.getActive(managerId, memberId)).thenReturn(Optional.empty());

		mockMvc.perform(get("/api/v1/manager/members/{memberId}/coach-assignment", memberId)
				.contextPath("/api/v1")
				.with(managerJwt()))
				.andExpect(status().isNoContent());
	}

	@Test
	void listHistory_returns200OkWithPagedContent() throws Exception {
		CoachAssignmentResponse response = sampleResponse(CoachAssignmentStatus.ACTIVE);
		when(service.listHistory(eq(managerId), eq(memberId), any(Pageable.class)))
				.thenReturn(new CoachAssignmentPageResponse(List.of(response), 0, 20, 1, 1));

		mockMvc.perform(get("/api/v1/manager/members/{memberId}/coach-assignments", memberId)
				.contextPath("/api/v1")
				.with(managerJwt())
				.queryParam("page", "0")
				.queryParam("size", "20"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].id").value(assignmentId.toString()))
				.andExpect(jsonPath("$.totalElements").value(1));
	}

	@Test
	void listHistory_whenInvalidPagination_returns400() throws Exception {
		mockMvc.perform(get("/api/v1/manager/members/{memberId}/coach-assignments", memberId)
				.contextPath("/api/v1")
				.with(managerJwt())
				.queryParam("page", "-1"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
	}

	@Test
	void end_whenValid_returns200Ok() throws Exception {
		CoachAssignmentResponse response = sampleResponse(CoachAssignmentStatus.ENDED);
		when(service.endAssignment(eq(managerId), eq(memberId), any(CoachAssignmentEndRequest.class)))
				.thenReturn(response);

		mockMvc.perform(patch("/api/v1/manager/members/{memberId}/coach-assignment/end", memberId)
				.contextPath("/api/v1")
				.with(managerJwt())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"reason\":\"Member requested reassignment\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(assignmentId.toString()))
				.andExpect(jsonPath("$.status").value("ENDED"));
	}

	@Test
	void end_whenBlankReason_returns400BadRequest() throws Exception {
		mockMvc.perform(patch("/api/v1/manager/members/{memberId}/coach-assignment/end", memberId)
				.contextPath("/api/v1")
				.with(managerJwt())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"reason\":\"   \"}"))
				.andExpect(status().isBadRequest());
	}

	private JwtRequestPostProcessor memberJwt() {
		return jwt().jwt(jwt -> jwt.subject(UUID.randomUUID().toString()))
				.authorities(new SimpleGrantedAuthority("ROLE_MEMBER"));
	}

	private JwtRequestPostProcessor coachJwt() {
		return jwt().jwt(jwt -> jwt.subject(UUID.randomUUID().toString()))
				.authorities(new SimpleGrantedAuthority("ROLE_COACH"));
	}

	@Test
	void assign_withoutToken_returns401Unauthorized() throws Exception {
		mockMvc.perform(post("/api/v1/manager/members/{memberId}/coach-assignment", memberId)
				.contextPath("/api/v1")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"coachAccountId\":\"" + coachId + "\"}"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void assign_withMemberRole_returns403Forbidden() throws Exception {
		mockMvc.perform(post("/api/v1/manager/members/{memberId}/coach-assignment", memberId)
				.contextPath("/api/v1")
				.with(memberJwt())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"coachAccountId\":\"" + coachId + "\"}"))
				.andExpect(status().isForbidden());
	}

	@Test
	void assign_withCoachRole_returns403Forbidden() throws Exception {
		mockMvc.perform(post("/api/v1/manager/members/{memberId}/coach-assignment", memberId)
				.contextPath("/api/v1")
				.with(coachJwt())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"coachAccountId\":\"" + coachId + "\"}"))
				.andExpect(status().isForbidden());
	}

	@Test
	void getActive_withoutToken_returns401Unauthorized() throws Exception {
		mockMvc.perform(get("/api/v1/manager/members/{memberId}/coach-assignment", memberId)
				.contextPath("/api/v1"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void getActive_withMemberRole_returns403Forbidden() throws Exception {
		mockMvc.perform(get("/api/v1/manager/members/{memberId}/coach-assignment", memberId)
				.contextPath("/api/v1")
				.with(memberJwt()))
				.andExpect(status().isForbidden());
	}

	@Test
	void getActive_withCoachRole_returns403Forbidden() throws Exception {
		mockMvc.perform(get("/api/v1/manager/members/{memberId}/coach-assignment", memberId)
				.contextPath("/api/v1")
				.with(coachJwt()))
				.andExpect(status().isForbidden());
	}

	@Test
	void listHistory_withoutToken_returns401Unauthorized() throws Exception {
		mockMvc.perform(get("/api/v1/manager/members/{memberId}/coach-assignments", memberId)
				.contextPath("/api/v1"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void listHistory_withMemberRole_returns403Forbidden() throws Exception {
		mockMvc.perform(get("/api/v1/manager/members/{memberId}/coach-assignments", memberId)
				.contextPath("/api/v1")
				.with(memberJwt()))
				.andExpect(status().isForbidden());
	}

	@Test
	void listHistory_withCoachRole_returns403Forbidden() throws Exception {
		mockMvc.perform(get("/api/v1/manager/members/{memberId}/coach-assignments", memberId)
				.contextPath("/api/v1")
				.with(coachJwt()))
				.andExpect(status().isForbidden());
	}

	@Test
	void end_withoutToken_returns401Unauthorized() throws Exception {
		mockMvc.perform(patch("/api/v1/manager/members/{memberId}/coach-assignment/end", memberId)
				.contextPath("/api/v1")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"reason\":\"Terminated\"}"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void end_withMemberRole_returns403Forbidden() throws Exception {
		mockMvc.perform(patch("/api/v1/manager/members/{memberId}/coach-assignment/end", memberId)
				.contextPath("/api/v1")
				.with(memberJwt())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"reason\":\"Terminated\"}"))
				.andExpect(status().isForbidden());
	}

	@Test
	void end_withCoachRole_returns403Forbidden() throws Exception {
		mockMvc.perform(patch("/api/v1/manager/members/{memberId}/coach-assignment/end", memberId)
				.contextPath("/api/v1")
				.with(coachJwt())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"reason\":\"Terminated\"}"))
				.andExpect(status().isForbidden());
	}
}
