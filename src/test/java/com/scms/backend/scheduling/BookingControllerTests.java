package com.scms.backend.scheduling;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
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
}
