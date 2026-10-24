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

@WebMvcTest(controllers = CoachLeaveController.class)
class CoachLeaveIdentityControllerTests {

	@Autowired MockMvc mockMvc;
	@MockitoBean CoachLeaveService service;

	@Test
	void managerListReturnsCoachIdentityWithoutAdditionalRequests() throws Exception {
		UUID managerId = UUID.randomUUID();
		UUID requestId = UUID.randomUUID();
		UUID coachId = UUID.randomUUID();
		Instant start = Instant.parse("2030-01-01T02:00:00Z");
		when(service.managerList(managerId, null)).thenReturn(List.of(new CoachLeaveResponse(requestId, coachId,
			"Nguyen Van Coach", "0900000000", start, start.plusSeconds(3600), "Leave",
			CoachLeaveStatus.PENDING, null, null, null, start.minusSeconds(3600), List.of())));

		mockMvc.perform(get("/api/v1/manager/coach-leave-requests")
				.contextPath("/api/v1")
				.with(jwt().jwt(token -> token.subject(managerId.toString()).claim("role", "MANAGER"))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].coachId").value(coachId.toString()))
			.andExpect(jsonPath("$[0].coachFullName").value("Nguyen Van Coach"))
			.andExpect(jsonPath("$[0].coachPhone").value("0900000000"));
	}
}
