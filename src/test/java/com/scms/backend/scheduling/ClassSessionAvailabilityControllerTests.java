package com.scms.backend.scheduling;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ClassSessionController.class)
class ClassSessionAvailabilityControllerTests {

	@Autowired MockMvc mockMvc;
	@MockitoBean ClassSessionService sessionService;
	@MockitoBean ClassSessionCancellationService cancellationService;

	@Test
	void managerCanListAvailableCoachesForSession() throws Exception {
		UUID managerId = UUID.randomUUID();
		UUID sessionId = UUID.randomUUID();
		UUID coachId = UUID.randomUUID();
		when(sessionService.availableCoaches(managerId, sessionId)).thenReturn(List.of(
			new AvailableCoachResponse(coachId, "Nguyen Van A", "0900000000")));

		mockMvc.perform(get("/api/v1/manager/class-sessions/{id}/available-coaches", sessionId)
				.contextPath("/api/v1")
				.with(jwt().jwt(token -> token.subject(managerId.toString()).claim("role", "MANAGER"))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].coachAccountId").value(coachId.toString()))
			.andExpect(jsonPath("$[0].fullName").value("Nguyen Van A"))
			.andExpect(jsonPath("$[0].phone").value("0900000000"));
	}
}
