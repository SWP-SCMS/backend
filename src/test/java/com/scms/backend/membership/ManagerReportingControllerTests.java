package com.scms.backend.membership;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ManagerReportingController.class)
class ManagerReportingControllerTests {

	@Autowired MockMvc mockMvc;
	@MockitoBean ManagerReportingService service;

	@Test
	void returnsTypedBookingReport() throws Exception {
		UUID managerId = UUID.randomUUID();
		Instant from = Instant.parse("2026-10-01T00:00:00Z");
		Instant to = Instant.parse("2026-10-02T00:00:00Z");
		when(service.bookings(managerId, from, to, false))
			.thenReturn(new BookingReportResponse(from, to, 5, 3, 2, false));

		mockMvc.perform(get("/api/v1/manager/reports/bookings").contextPath("/api/v1")
			.with(jwt().jwt(jwt -> jwt.subject(managerId.toString()).claim("role", "MANAGER")))
			.queryParam("from", from.toString()).queryParam("to", to.toString()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.totalBookings").value(5))
			.andExpect(jsonPath("$.bookedBookings").value(3))
			.andExpect(jsonPath("$.cancelledBookings").value(2));
	}

	@Test
	void reportsValidationErrorsWithStableProblemCode() throws Exception {
		when(service.members(any(UUID.class), any(Instant.class), any(Instant.class), eq(false)))
			.thenThrow(new ReportingValidationException("to", "must be later than from"));

		mockMvc.perform(get("/api/v1/manager/reports/members").contextPath("/api/v1")
			.with(jwt().jwt(jwt -> jwt.subject(UUID.randomUUID().toString()).claim("role", "MANAGER")))
			.queryParam("from", "2026-10-02T00:00:00Z").queryParam("to", "2026-10-01T00:00:00Z"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
			.andExpect(jsonPath("$.errors.to[0]").value("must be later than from"));
	}

}
