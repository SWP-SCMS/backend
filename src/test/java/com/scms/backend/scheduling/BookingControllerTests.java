package com.scms.backend.scheduling;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

@WebMvcTest(controllers = BookingController.class)
class BookingControllerTests {

	@Autowired
	MockMvc mockMvc;

	@MockitoBean
	BookingService service;

	@Test
	void memberCanReadOwnBookings() throws Exception {
		mockMvc.perform(get("/api/v1/members/me/bookings")
			.contextPath("/api/v1")
			.with(jwt().jwt(jwt -> jwt.subject(UUID.randomUUID().toString()).claim("role", "MEMBER"))))
			.andExpect(status().isOk());
	}

	@Test
	void receptionistCanReadBookingsForAMember() throws Exception {
		mockMvc.perform(get("/api/v1/reception/members/{memberId}/bookings", UUID.randomUUID())
			.contextPath("/api/v1")
			.with(jwt().jwt(jwt -> jwt.subject(UUID.randomUUID().toString()).claim("role", "RECEPTIONIST"))))
			.andExpect(status().isOk());
	}

	@Test
	void memberBenefitFailureUsesStructuredConflictContract() throws Exception {
		UUID memberId = UUID.randomUUID();
		UUID sessionId = UUID.randomUUID();
		org.mockito.Mockito.when(service.book(memberId, sessionId)).thenThrow(
			BookingException.benefitRequired(List.of("CLASS_BOOKING", "POOL_ACCESS"),
				List.of("POOL_ACCESS")));

		mockMvc.perform(post("/api/v1/members/class-sessions/{sessionId}/bookings", sessionId)
				.contextPath("/api/v1")
				.with(jwt().jwt(token -> token.subject(memberId.toString()).claim("role", "MEMBER"))))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("MEMBERSHIP_BENEFIT_REQUIRED"))
			.andExpect(jsonPath("$.requiredBenefitCodes[0]").value("CLASS_BOOKING"))
			.andExpect(jsonPath("$.requiredBenefitCodes[1]").value("POOL_ACCESS"))
			.andExpect(jsonPath("$.missingBenefitCodes[0]").value("POOL_ACCESS"));
	}

	@Test
	void receptionistBenefitFailureUsesStructuredConflictContract() throws Exception {
		UUID receptionistId = UUID.randomUUID();
		UUID memberId = UUID.randomUUID();
		UUID sessionId = UUID.randomUUID();
		org.mockito.Mockito.when(service.bookForMember(receptionistId, memberId, sessionId)).thenThrow(
			BookingException.benefitRequired(List.of("CLASS_BOOKING"), List.of("CLASS_BOOKING")));

		mockMvc.perform(post("/api/v1/reception/members/{memberId}/bookings", memberId)
				.contextPath("/api/v1")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"sessionId\":\"" + sessionId + "\"}")
				.with(jwt().jwt(token -> token.subject(receptionistId.toString())
					.claim("role", "RECEPTIONIST"))))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("MEMBERSHIP_BENEFIT_REQUIRED"))
			.andExpect(jsonPath("$.requiredBenefitCodes[0]").value("CLASS_BOOKING"))
			.andExpect(jsonPath("$.missingBenefitCodes[0]").value("CLASS_BOOKING"));
	}
}
