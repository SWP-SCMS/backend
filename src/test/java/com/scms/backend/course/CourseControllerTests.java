package com.scms.backend.course;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;

import com.scms.backend.auth.ActiveAccountJwtValidator;
import com.scms.backend.common.error.SecurityProblemDetailHandler;
import com.scms.backend.config.SecurityConfiguration;
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

@WebMvcTest(controllers = CourseController.class)
@AutoConfigureMockMvc
@Import({ SecurityConfiguration.class, SecurityProblemDetailHandler.class })
@TestPropertySource(properties = {
	"scms.cors.allowed-origins=http://localhost:5173",
	"scms.security.jwt-secret=test-only-jwt-secret-at-least-32-bytes-long"
})
class CourseControllerTests {

	@Autowired MockMvc mockMvc;
	@MockitoBean CourseManagementService service;
	@MockitoBean ActiveAccountJwtValidator activeAccountJwtValidator;

	@Test
	void managerCreatesCourse() throws Exception {
		UUID managerId = UUID.randomUUID();
		UUID courseId = UUID.randomUUID();
		UUID disciplineId = UUID.randomUUID();
		Instant now = Instant.parse("2026-10-30T00:00:00Z");
		when(service.createCourse(eq(managerId), any())).thenReturn(new CourseResponse(courseId, "SWIM-101",
			"Beginner Swim", null, disciplineId, "Swimming", null, 3, CourseStatus.DRAFT, now, now, 0L));

		mockMvc.perform(post("/api/v1/manager/courses").contextPath("/api/v1")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"code":"SWIM-101","name":"Beginner Swim","disciplineId":"%s",
					 "defaultMinimumEnrollment":3}
					""".formatted(disciplineId))
				.with(jwt().jwt(token -> token.subject(managerId.toString()).claim("role", "MANAGER"))
					.authorities(new SimpleGrantedAuthority("ROLE_MANAGER"))))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.id").value(courseId.toString()))
			.andExpect(jsonPath("$.status").value("DRAFT"))
			.andExpect(jsonPath("$.version").value(0));
	}

	@Test
	void blankCourseFieldsFailBeforeServiceInvocation() throws Exception {
		mockMvc.perform(post("/api/v1/manager/courses").contextPath("/api/v1")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"code\":\" \",\"name\":\" \",\"disciplineId\":null}")
				.with(jwt().jwt(token -> token.subject(UUID.randomUUID().toString()).claim("role", "MANAGER"))
					.authorities(new SimpleGrantedAuthority("ROLE_MANAGER"))))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
			.andExpect(jsonPath("$.errors.code").isArray())
			.andExpect(jsonPath("$.errors.name").isArray())
			.andExpect(jsonPath("$.errors.disciplineId").isArray());
		verifyNoInteractions(service);
	}

	@Test
	void malformedCourseUuidReturnsProblemDetailInsteadOfServerError() throws Exception {
		mockMvc.perform(get("/api/v1/manager/courses/not-a-uuid").contextPath("/api/v1")
				.with(jwt().jwt(token -> token.subject(UUID.randomUUID().toString()).claim("role", "MANAGER"))
					.authorities(new SimpleGrantedAuthority("ROLE_MANAGER"))))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
		verifyNoInteractions(service);
	}

	@Test
	void courseExceptionUsesStableProblemCode() throws Exception {
		UUID managerId = UUID.randomUUID();
		UUID courseId = UUID.randomUUID();
		when(service.getCourse(managerId, courseId)).thenThrow(CourseException.notFound());

		mockMvc.perform(get("/api/v1/manager/courses/{courseId}", courseId).contextPath("/api/v1")
				.with(jwt().jwt(token -> token.subject(managerId.toString()).claim("role", "MANAGER"))
					.authorities(new SimpleGrantedAuthority("ROLE_MANAGER"))))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("COURSE_NOT_FOUND"))
			.andExpect(jsonPath("$.detail").value("The requested course was not found."));
	}

	@Test
	void nonManagerCannotReadCourses() throws Exception {
		mockMvc.perform(get("/api/v1/manager/courses").contextPath("/api/v1")
				.with(jwt().jwt(token -> token.subject(UUID.randomUUID().toString()).claim("role", "MEMBER"))
					.authorities(new SimpleGrantedAuthority("ROLE_MEMBER"))))
			.andExpect(status().isForbidden());
		verifyNoInteractions(service);
	}
}
