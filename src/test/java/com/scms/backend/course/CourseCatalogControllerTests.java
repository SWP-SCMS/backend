package com.scms.backend.course;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigInteger;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.scms.backend.auth.ActiveAccountJwtValidator;
import com.scms.backend.common.error.SecurityProblemDetailHandler;
import com.scms.backend.config.SecurityConfiguration;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = CourseCatalogController.class)
@AutoConfigureMockMvc
@Import({ SecurityConfiguration.class, SecurityProblemDetailHandler.class })
@TestPropertySource(properties = {
	"scms.cors.allowed-origins=http://localhost:5173",
	"scms.security.jwt-secret=test-only-jwt-secret-at-least-32-bytes-long"
})
class CourseCatalogControllerTests {

	@Autowired MockMvc mockMvc;
	@MockitoBean CourseCatalogService service;
	@MockitoBean ActiveAccountJwtValidator activeAccountJwtValidator;

	@Test
	void anonymousCourseListUsesBoundedDeterministicPagination() throws Exception {
		when(service.listCourses(any())).thenReturn(new CatalogCoursePageResponse(List.of(), 0, 20, 0, 0));

		mockMvc.perform(get("/api/v1/courses").contextPath("/api/v1"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content").isEmpty());

		ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
		verify(service).listCourses(pageable.capture());
		assertThat(pageable.getValue().getPageNumber()).isZero();
		assertThat(pageable.getValue().getPageSize()).isEqualTo(20);
		assertThat(pageable.getValue().getSort().toString()).isEqualTo("name: ASC,id: ASC");
	}

	@Test
	void runListSortsByPlannedStartThenId() throws Exception {
		when(service.listRuns(any())).thenReturn(new CatalogCourseRunPageResponse(List.of(), 0, 5, 0, 0));

		mockMvc.perform(get("/api/v1/course-runs?page=0&size=5").contextPath("/api/v1"))
			.andExpect(status().isOk());

		ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
		verify(service).listRuns(pageable.capture());
		assertThat(pageable.getValue().getSort().toString()).isEqualTo("plannedStartAt: ASC,id: ASC");
	}

	@Test
	void invalidPaginationReturnsStableValidationProblem() throws Exception {
		for (String query : new String[] { "?page=-1", "?size=0", "?size=101" }) {
			mockMvc.perform(get("/api/v1/courses" + query).contextPath("/api/v1"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("COURSE_VALIDATION_ERROR"));
		}
		verifyNoInteractions(service);
	}

	@Test
	void malformedCatalogUuidReturnsSharedProblem() throws Exception {
		mockMvc.perform(get("/api/v1/course-runs/not-a-uuid").contextPath("/api/v1"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
		verifyNoInteractions(service);
	}

	@Test
	void invalidBearerTokenIsRejectedOnPublicCatalog() throws Exception {
		mockMvc.perform(get("/api/v1/courses").contextPath("/api/v1")
				.header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt"))
			.andExpect(status().isUnauthorized());
		verifyNoInteractions(service);
	}

	@Test
	void runDetailOmitsInternalAndSeatAvailabilityFields() throws Exception {
		UUID runId = UUID.randomUUID();
		Instant opens = Instant.parse("2026-11-01T00:00:00Z");
		Instant closes = Instant.parse("2026-12-01T00:00:00Z");
		CatalogCourseRunResponse run = new CatalogCourseRunResponse(runId, "RUN-1", UUID.randomUUID(),
			"SWIM-101", "Beginner Swim", UUID.randomUUID(), "Coach One", BigInteger.valueOf(1500000), 20,
			opens, closes, RegistrationStatus.OPEN, closes.plusSeconds(3600), closes.plusSeconds(86400), 8,
			CourseRunStatus.OPEN_FOR_REGISTRATION);
		when(service.getRun(runId)).thenReturn(new CatalogCourseRunDetailResponse(run, List.of()));

		mockMvc.perform(get("/api/v1/course-runs/{runId}", runId).contextPath("/api/v1"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(runId.toString()))
			.andExpect(jsonPath("$.registrationStatus").value("OPEN"))
			.andExpect(jsonPath("$.sessions").isEmpty())
			.andExpect(jsonPath("$.capacityRemaining").doesNotExist())
			.andExpect(jsonPath("$.minimumEnrollment").doesNotExist())
			.andExpect(jsonPath("$.policyVersionSnapshot").doesNotExist())
			.andExpect(jsonPath("$.createdAt").doesNotExist())
			.andExpect(jsonPath("$.updatedAt").doesNotExist())
			.andExpect(jsonPath("$.version").doesNotExist());
	}
}
