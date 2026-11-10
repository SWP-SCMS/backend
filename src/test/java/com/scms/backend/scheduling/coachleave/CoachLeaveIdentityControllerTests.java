package com.scms.backend.scheduling.coachleave;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.scms.backend.scheduling.CoachLeaveStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = CoachLeaveController.class)
class CoachLeaveIdentityControllerTests {

	@Autowired
	MockMvc mockMvc;

	@MockitoBean
	CoachLeaveService service;

	@Test
	void createAcceptsIsoInstantsWithoutAmbiguousDayMonthParsing() throws Exception {
		UUID coachId = UUID.randomUUID();
		var request = new CoachLeaveCreateRequest(Instant.parse("2030-11-09T01:00:00Z"), Instant.parse("2030-11-09T02:00:00Z"), "Leave");
		mockMvc.perform(post("/api/v1/coach/leave-requests").contextPath("/api/v1").with(jwt().jwt(token -> token.subject(coachId.toString()).claim("role", "COACH"))).contentType(MediaType.APPLICATION_JSON).content("{\"startTime\":\"2030-11-09T01:00:00.000Z\",\"endTime\":\"2030-11-09T02:00:00.000Z\",\"reason\":\"Leave\"}")).andExpect(status().isCreated());
		verify(service).create(coachId, request);
	}

	@Test
	void createRejectsSlashFormattedDatesRatherThanGuessingDayAndMonth() throws Exception {
		for (String date : List.of("11/09/2030 08:00", "09/11/2030 08:00")) {
			mockMvc.perform(post("/api/v1/coach/leave-requests").contextPath("/api/v1").with(jwt().jwt(token -> token.subject(UUID.randomUUID().toString()).claim("role", "COACH"))).contentType(MediaType.APPLICATION_JSON).content("{\"startTime\":\"" + date + "\",\"endTime\":\"2030-11-09T02:00:00Z\",\"reason\":\"Leave\"}")).andExpect(status().isBadRequest());
		}
		verifyNoInteractions(service);
	}

	@Test
	void managerListReturnsCoachIdentityWithoutAdditionalRequests() throws Exception {
		UUID managerId = UUID.randomUUID();
		UUID requestId = UUID.randomUUID();
		UUID coachId = UUID.randomUUID();
		Instant start = Instant.parse("2030-01-01T02:00:00Z");
		when(service.managerList(managerId, null)).thenReturn(List.of(new CoachLeaveResponse(requestId, coachId, "Nguyen Van Coach", "0900000000", start, start.plusSeconds(3600), "Leave", CoachLeaveStatus.PENDING, null, null, null, start.minusSeconds(3600), List.of())));
		mockMvc.perform(get("/api/v1/manager/coach-leave-requests").contextPath("/api/v1").with(jwt().jwt(token -> token.subject(managerId.toString()).claim("role", "MANAGER")))).andExpect(status().isOk()).andExpect(jsonPath("$[0].coachId").value(coachId.toString())).andExpect(jsonPath("$[0].coachFullName").value("Nguyen Van Coach")).andExpect(jsonPath("$[0].coachPhone").value("0900000000"));
	}
}
