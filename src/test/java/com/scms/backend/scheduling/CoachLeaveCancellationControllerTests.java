package com.scms.backend.scheduling;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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

@WebMvcTest(controllers = CoachLeaveController.class)
class CoachLeaveCancellationControllerTests {

	@Autowired MockMvc mockMvc;
	@MockitoBean CoachLeaveService service;

	@Test
	void coachCancelsApprovedLeave() throws Exception {
		UUID coachId = UUID.randomUUID(); UUID requestId = UUID.randomUUID(); UUID managerId = UUID.randomUUID();
		Instant start = Instant.parse("2030-01-01T02:00:00Z");
		when(service.cancel(coachId, requestId)).thenReturn(new CoachLeaveResponse(requestId, coachId,
			"Coach A", "0900000000", start, start.plusSeconds(3600), "Leave", CoachLeaveStatus.CANCELLED,
			managerId, "Approved", Instant.parse("2029-12-01T00:00:00Z"),
			Instant.parse("2029-11-01T00:00:00Z"), List.of()));

		mockMvc.perform(patch("/api/v1/coach/leave-requests/{requestId}/cancel", requestId)
				.contextPath("/api/v1")
				.with(jwt().jwt(token -> token.subject(coachId.toString()).claim("role", "COACH"))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(requestId.toString()))
			.andExpect(jsonPath("$.status").value("CANCELLED"));
	}

	@Test
	void startedLeaveReturnsConflictCode() throws Exception {
		UUID coachId = UUID.randomUUID(); UUID requestId = UUID.randomUUID();
		when(service.cancel(coachId, requestId)).thenThrow(CoachLeaveException.cancellationAlreadyStarted());

		mockMvc.perform(patch("/api/v1/coach/leave-requests/{requestId}/cancel", requestId)
				.contextPath("/api/v1")
				.with(jwt().jwt(token -> token.subject(coachId.toString()).claim("role", "COACH"))))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("COACH_LEAVE_ALREADY_STARTED"));
	}
}
