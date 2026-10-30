package com.scms.backend.membership;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SepayController.class)
@AutoConfigureMockMvc
class SepayControllerCoursePaymentRouteTests {

	@Autowired MockMvc mockMvc;
	@MockitoBean SepayService service;

	@Test
	void memberCanCreateOrResumeCoursePaymentWithAnIdempotencyKey() throws Exception {
		mockMvc.perform(post("/members/me/course-orders/{orderId}/payments/sepay", UUID.randomUUID())
				.with(jwt().jwt(token -> token.subject(UUID.randomUUID().toString())))
				.header("Idempotency-Key", "course-payment-retry")
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk());
	}
}
