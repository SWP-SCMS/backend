package com.scms.backend.course.management;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.scms.backend.auth.ActiveAccountJwtValidator;
import com.scms.backend.common.error.SecurityProblemDetailHandler;
import com.scms.backend.config.SecurityConfiguration;
import com.scms.backend.course.CourseStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = CourseApprovalController.class)
@AutoConfigureMockMvc
@Import({ SecurityConfiguration.class, SecurityProblemDetailHandler.class })
@TestPropertySource(properties = { "scms.cors.allowed-origins=http://localhost:5173", "scms.security.jwt-secret=test-only-jwt-secret-at-least-32-bytes-long" })
class CourseApprovalControllerTests {

	@Autowired
	MockMvc mockMvc;

	@MockitoBean
	CourseManagementService service;

	@MockitoBean
	ActiveAccountJwtValidator activeAccountJwtValidator;

	@Test
	void receptionistCannotSubmitCourseAndServiceIsNotInvoked() throws Exception {
		UUID receptionistId = UUID.randomUUID();
		UUID disciplineId = UUID.randomUUID();
		mockMvc.perform(post("/api/v1/reception/courses").contextPath("/api/v1").contentType(MediaType.APPLICATION_JSON).content("""
			{"code":"SWIM-APPROVAL","name":"Beginner Swim","disciplineId":"%s",
			 "defaultMinimumEnrollment":3}
			""".formatted(disciplineId)).with(actor(receptionistId, "RECEPTIONIST"))).andExpect(status().isForbidden());
		verifyNoInteractions(service);
	}

	@Test
	void managerReadsQueueAndApprovesWithVersionedReason() throws Exception {
		UUID managerId = UUID.randomUUID();
		CourseApprovalResponse pending = response(CourseStatus.PENDING_APPROVAL, UUID.randomUUID());
		CourseApprovalResponse approved = new CourseApprovalResponse(pending.id(), pending.code(), pending.name(), pending.description(), pending.disciplineId(), pending.disciplineName(), pending.imageUrl(), pending.defaultMinimumEnrollment(), CourseStatus.DRAFT, pending.submittedByAccountId(), pending.submittedAt(), managerId, Instant.parse("2026-11-01T01:00:00Z"), "Verified", pending.createdAt(), pending.updatedAt(), 1L);
		when(service.listCourseApprovals(eq(managerId), any())).thenReturn(new CourseApprovalPageResponse(List.of(pending), 0, 20, 1, 1));
		when(service.approveCourse(eq(managerId), eq(pending.id()), any())).thenReturn(approved);
		mockMvc.perform(get("/api/v1/manager/course-approval-requests").contextPath("/api/v1").with(actor(managerId, "MANAGER"))).andExpect(status().isOk()).andExpect(jsonPath("$.content[0].id").value(pending.id().toString()));
		mockMvc.perform(post("/api/v1/manager/course-approval-requests/{courseId}/approve", pending.id()).contextPath("/api/v1").contentType(MediaType.APPLICATION_JSON).content("{\"expectedVersion\":0,\"reason\":\"Verified\"}").with(actor(managerId, "MANAGER"))).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("DRAFT")).andExpect(jsonPath("$.reviewReason").value("Verified"));
	}

	@Test
	void wrongRolesAndBlankDecisionAreRejectedBeforeServiceInvocation() throws Exception {
		UUID memberId = UUID.randomUUID();
		UUID courseId = UUID.randomUUID();
		mockMvc.perform(post("/api/v1/reception/courses").contextPath("/api/v1").contentType(MediaType.APPLICATION_JSON).content("{\"code\":\"C\",\"name\":\"Course\",\"disciplineId\":\"%s\"}".formatted(UUID.randomUUID())).with(actor(memberId, "MEMBER"))).andExpect(status().isForbidden());
		mockMvc.perform(post("/api/v1/manager/course-approval-requests/{courseId}/reject", courseId).contextPath("/api/v1").contentType(MediaType.APPLICATION_JSON).content("{\"expectedVersion\":0,\"reason\":\" \"}").with(actor(UUID.randomUUID(), "MANAGER"))).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
		verifyNoInteractions(service);
	}

	private CourseApprovalResponse response(CourseStatus status, UUID receptionistId) {
		Instant now = Instant.parse("2026-11-01T00:00:00Z");
		return new CourseApprovalResponse(UUID.randomUUID(), "SWIM-APPROVAL", "Beginner Swim", null, UUID.randomUUID(), "Swimming", null, 3, status, receptionistId, now, null, null, null, now, now, 0L);
	}

	private org.springframework.test.web.servlet.request.RequestPostProcessor actor(UUID id, String role) {
		return jwt().jwt(token -> token.subject(id.toString()).claim("role", role)).authorities(new SimpleGrantedAuthority("ROLE_" + role));
	}
}
