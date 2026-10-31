package com.scms.backend.course;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
	@MockitoBean CourseSessionDisruptionService disruptions;
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
	void acceptsCourseDescriptionAndImageAtDocumentedLimits() throws Exception {
		UUID managerId = UUID.randomUUID();
		UUID courseId = UUID.randomUUID();
		UUID disciplineId = UUID.randomUUID();
		Instant now = Instant.parse("2026-10-30T00:00:00Z");
		String description = "d".repeat(2000);
		String imageUrl = "i".repeat(500);
		when(service.createCourse(eq(managerId), any())).thenReturn(new CourseResponse(courseId, "SWIM-101",
			"Beginner Swim", description, disciplineId, "Swimming", imageUrl, null, CourseStatus.DRAFT,
			now, now, 0L));

		mockMvc.perform(post("/api/v1/manager/courses").contextPath("/api/v1")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"code":"SWIM-101","name":"Beginner Swim","description":"%s",
					 "disciplineId":"%s","imageUrl":"%s"}
					""".formatted(description, disciplineId, imageUrl))
				.with(manager(managerId)))
			.andExpect(status().isCreated());
	}

	@Test
	void rejectsCourseDescriptionAndImageOverDocumentedLimits() throws Exception {
		UUID disciplineId = UUID.randomUUID();
		UUID managerId = UUID.randomUUID();
		for (String body : new String[] {
			"""
			{"code":"SWIM-101","name":"Beginner Swim","description":"%s","disciplineId":"%s"}
			""".formatted("d".repeat(2001), disciplineId),
			"""
			{"code":"SWIM-101","name":"Beginner Swim","disciplineId":"%s","imageUrl":"%s"}
			""".formatted(disciplineId, "i".repeat(501))
		}) {
			mockMvc.perform(post("/api/v1/manager/courses").contextPath("/api/v1")
					.contentType(MediaType.APPLICATION_JSON).content(body).with(manager(managerId)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
		}
		verifyNoInteractions(service);
	}

	@Test
	void rejectsOverlongCoursePatchText() throws Exception {
		UUID managerId = UUID.randomUUID();
		mockMvc.perform(patch("/api/v1/manager/courses/{courseId}", UUID.randomUUID()).contextPath("/api/v1")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"expectedVersion\":0,\"description\":\"%s\"}".formatted("d".repeat(2001)))
				.with(manager(managerId)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
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
	void missingCourseForRunCreationUsesCourseNotFoundProblem() throws Exception {
		UUID managerId = UUID.randomUUID();
		UUID courseId = UUID.randomUUID();
		UUID coachId = UUID.randomUUID();
		when(service.createRun(eq(managerId), any())).thenThrow(CourseException.notFound());

		mockMvc.perform(post("/api/v1/manager/course-runs").contextPath("/api/v1")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"runCode":"RUN-1","courseId":"%s","primaryCoachId":"%s","tuitionVnd":1000000,
					 "capacity":20,"minimumEnrollment":2,"registrationOpensAt":"2030-01-01T00:00:00Z",
					 "registrationClosesAt":"2030-01-15T00:00:00Z","plannedStartAt":"2030-02-01T00:00:00Z",
					 "plannedEndAt":"2030-03-01T00:00:00Z","totalUnits":10,"namedCoachCommitment":true}
					""".formatted(courseId, coachId))
				.with(manager(managerId)))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("COURSE_NOT_FOUND"));
	}

	@Test
	void nonManagerCannotReadCourses() throws Exception {
		mockMvc.perform(get("/api/v1/manager/courses").contextPath("/api/v1")
				.with(jwt().jwt(token -> token.subject(UUID.randomUUID().toString()).claim("role", "MEMBER"))
					.authorities(new SimpleGrantedAuthority("ROLE_MEMBER"))))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("FORBIDDEN"));
		verifyNoInteractions(service);
	}

	@Test
	void anonymousCourseRequestUsesActualAuthenticationProblemCode() throws Exception {
		mockMvc.perform(get("/api/v1/manager/courses").contextPath("/api/v1"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
		verifyNoInteractions(service);
	}

	private org.springframework.test.web.servlet.request.RequestPostProcessor manager(UUID managerId) {
		return jwt().jwt(token -> token.subject(managerId.toString()).claim("role", "MANAGER"))
			.authorities(new SimpleGrantedAuthority("ROLE_MANAGER"));
	}
}
