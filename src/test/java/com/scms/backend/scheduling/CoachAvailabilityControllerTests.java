package com.scms.backend.scheduling;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = CoachAvailabilityController.class)
class CoachAvailabilityControllerTests {

	@Autowired MockMvc mockMvc;
	@MockitoBean ClassSessionService service;

	@Test
	void discoversCoachAvailableAcrossAllRequestedWindows() throws Exception {
		UUID managerId = UUID.randomUUID();
		UUID coachId = UUID.randomUUID();
		Instant firstStart = Instant.parse("2032-01-01T02:00:00Z");
		Instant firstEnd = Instant.parse("2032-01-01T03:00:00Z");
		Instant secondStart = Instant.parse("2032-01-08T02:00:00Z");
		Instant secondEnd = Instant.parse("2032-01-08T03:00:00Z");
		when(service.availableCoaches(managerId, List.of(firstStart, secondStart),
			List.of(firstEnd, secondEnd), null)).thenReturn(List.of(
				new AvailableCoachResponse(coachId, "Nguyen Van A", "0900000000")));

		mockMvc.perform(get("/api/v1/manager/coaches/available")
				.contextPath("/api/v1")
				.param("startTime", firstStart.toString(), secondStart.toString())
				.param("endTime", firstEnd.toString(), secondEnd.toString())
				.with(jwt().jwt(token -> token.subject(managerId.toString()).claim("role", "MANAGER"))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].coachAccountId").value(coachId.toString()))
			.andExpect(jsonPath("$[0].fullName").value("Nguyen Van A"));
	}
}
