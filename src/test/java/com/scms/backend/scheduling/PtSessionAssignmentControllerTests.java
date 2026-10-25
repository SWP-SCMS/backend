package com.scms.backend.scheduling;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
class PtSessionAssignmentControllerTests {

	@Autowired MockMvc mockMvc;
	@MockitoBean ClassSessionService sessionService;
	@MockitoBean ClassSessionCancellationService cancellationService;

	@Test
	void incompatibleMembersReturnBookingAndWaitlistDetails() throws Exception {
		UUID managerId = UUID.randomUUID();
		UUID sessionId = UUID.randomUUID();
		UUID coachId = UUID.randomUUID();
		UUID bookingId = UUID.randomUUID();
		UUID bookingMemberId = UUID.randomUUID();
		UUID waitlistId = UUID.randomUUID();
		UUID waitlistMemberId = UUID.randomUUID();
		when(sessionService.updateAssignment(managerId, sessionId,
			new ClassSessionAssignmentRequest(coachId, null))).thenThrow(
				PtSessionAssignmentException.incompatibleMembers(
					List.of(new PtSessionAffectedBooking(bookingId, bookingMemberId, "MB-100001", "Booked Member")),
					List.of(new PtSessionAffectedWaitlist(waitlistId, waitlistMemberId, "MB-100002",
						"Waitlisted Member"))));

		mockMvc.perform(patch("/api/v1/manager/class-sessions/{id}/assignment", sessionId)
				.contextPath("/api/v1")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"coachId\":\"" + coachId + "\"}")
				.with(jwt().jwt(token -> token.subject(managerId.toString()).claim("role", "MANAGER"))))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("PT_SESSION_ASSIGNMENT_HAS_INCOMPATIBLE_MEMBERS"))
			.andExpect(jsonPath("$.affectedBookings[0].bookingId").value(bookingId.toString()))
			.andExpect(jsonPath("$.affectedBookings[0].memberAccountId").value(bookingMemberId.toString()))
			.andExpect(jsonPath("$.affectedBookings[0].memberCode").value("MB-100001"))
			.andExpect(jsonPath("$.affectedBookings[0].memberFullName").value("Booked Member"))
			.andExpect(jsonPath("$.affectedWaitlists[0].waitlistId").value(waitlistId.toString()))
			.andExpect(jsonPath("$.affectedWaitlists[0].memberAccountId").value(waitlistMemberId.toString()))
			.andExpect(jsonPath("$.affectedWaitlists[0].memberCode").value("MB-100002"))
			.andExpect(jsonPath("$.affectedWaitlists[0].memberFullName").value("Waitlisted Member"));
	}
}
