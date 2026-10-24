package com.scms.backend.scheduling;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = CoachLeaveController.class)
class CoachLeaveExpiryControllerTests {

	@Autowired MockMvc mockMvc;
	@MockitoBean CoachLeaveService service;

	@Test
	void approvingStartedLeaveReturnsSpecificConflict() throws Exception {
		UUID managerId = UUID.randomUUID();
		UUID requestId = UUID.randomUUID();
		when(service.approve(managerId, requestId, null)).thenThrow(CoachLeaveException.alreadyStarted());

		mockMvc.perform(patch("/api/v1/manager/coach-leave-requests/{requestId}/approve", requestId)
				.contextPath("/api/v1")
				.with(jwt().jwt(token -> token.subject(managerId.toString()).claim("role", "MANAGER"))))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("COACH_LEAVE_ALREADY_STARTED"))
			.andExpect(jsonPath("$.detail")
				.value("A leave request cannot be approved after its start time."));
	}
}
