package com.scms.backend.membership.reporting;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.scms.backend.course.CourseRunStatus;
import com.scms.backend.membership.ReportingValidationException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ManagerReportingController.class)
class ManagerReportingControllerTests {

	@Autowired
	MockMvc mockMvc;

	@MockitoBean
	ManagerReportingService service;

	@Test
	void returnsTypedBookingReport() throws Exception {
		UUID managerId = UUID.randomUUID();
		Instant from = Instant.parse("2026-10-01T00:00:00Z");
		Instant to = Instant.parse("2026-10-02T00:00:00Z");
		when(service.bookings(managerId, from, to, false)).thenReturn(new BookingReportResponse(from, to, 5, 3, 2, false));
		mockMvc.perform(get("/api/v1/manager/reports/bookings").contextPath("/api/v1").with(jwt().jwt(jwt -> jwt.subject(managerId.toString()).claim("role", "MANAGER"))).queryParam("from", from.toString()).queryParam("to", to.toString())).andExpect(status().isOk()).andExpect(jsonPath("$.totalBookings").value(5)).andExpect(jsonPath("$.bookedBookings").value(3)).andExpect(jsonPath("$.cancelledBookings").value(2));
	}

	@Test
	void reportsValidationErrorsWithStableProblemCode() throws Exception {
		when(service.members(any(UUID.class), any(Instant.class), any(Instant.class), eq(false))).thenThrow(new ReportingValidationException("to", "must be later than from"));
		mockMvc.perform(get("/api/v1/manager/reports/members").contextPath("/api/v1").with(jwt().jwt(jwt -> jwt.subject(UUID.randomUUID().toString()).claim("role", "MANAGER"))).queryParam("from", "2026-10-02T00:00:00Z").queryParam("to", "2026-10-01T00:00:00Z")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR")).andExpect(jsonPath("$.errors.to[0]").value("must be later than from"));
	}

	@Test
	void returnsBoundedFilteredCourseRunReportForManager() throws Exception {
		UUID managerId = UUID.randomUUID();
		LocalDate start = LocalDate.of(2026, 10, 1);
		LocalDate end = LocalDate.of(2026, 11, 1);
		when(service.courseRuns(managerId, null, CourseRunStatus.COMPLETED, start, end, 1, 10)).thenReturn(new CourseRunReportPageResponse(List.of(), 1, 10, 12, 2));
		mockMvc.perform(get("/api/v1/manager/reports/course-runs").contextPath("/api/v1").with(jwt().jwt(jwt -> jwt.subject(managerId.toString()).claim("role", "MANAGER"))).queryParam("status", "COMPLETED").queryParam("startDate", start.toString()).queryParam("endDate", end.toString()).queryParam("page", "1").queryParam("size", "10")).andExpect(status().isOk()).andExpect(jsonPath("$.content").isEmpty()).andExpect(jsonPath("$.page").value(1)).andExpect(jsonPath("$.size").value(10)).andExpect(jsonPath("$.totalElements").value(12)).andExpect(jsonPath("$.totalPages").value(2));
	}
}
