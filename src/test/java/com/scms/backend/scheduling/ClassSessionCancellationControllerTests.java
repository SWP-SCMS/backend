package com.scms.backend.scheduling;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ClassSessionController.class)
class ClassSessionCancellationControllerTests {

	@Autowired MockMvc mockMvc;
	@MockitoBean ClassSessionService sessionService;
	@MockitoBean ClassSessionCancellationService cancellationService;

	@Test
	void mixedExplicitSelectionReturnsConflictWithEligibleAndIneligibleIds() throws Exception {
		UUID managerId = UUID.randomUUID();
		UUID eligibleId = UUID.randomUUID();
		UUID ineligibleId = UUID.randomUUID();
		ClassSessionCancellationBatchRequest request = new ClassSessionCancellationBatchRequest(
			List.of(eligibleId, ineligibleId), null, null, null, "stale selection");
		when(cancellationService.cancelBulk(managerId, request))
			.thenThrow(RecurringScheduleException.bulkSelectionChanged(List.of(eligibleId), List.of(ineligibleId)));

		mockMvc.perform(post("/api/v1/manager/class-sessions/bulk-cancel")
				.contextPath("/api/v1")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"sessionIds":["%s","%s"],"reason":"stale selection"}
					""".formatted(eligibleId, ineligibleId))
				.with(jwt().jwt(token -> token.subject(managerId.toString()).claim("role", "MANAGER"))))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("SESSION_BULK_SELECTION_CHANGED"))
			.andExpect(jsonPath("$.eligibleSessionIds[0]").value(eligibleId.toString()))
			.andExpect(jsonPath("$.ineligibleSessionIds[0]").value(ineligibleId.toString()));
	}
}
