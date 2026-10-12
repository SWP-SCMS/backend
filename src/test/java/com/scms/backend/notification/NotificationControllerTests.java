package com.scms.backend.notification;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(NotificationController.class)
class NotificationControllerTests {

	@Autowired MockMvc mockMvc;
	@MockitoBean NotificationService service;

	@Test
	void listsOwnedNotificationsWithUnreadCount() throws Exception {
		UUID accountId = UUID.randomUUID();
		UUID notificationId = UUID.randomUUID();
		NotificationResponse response = new NotificationResponse(notificationId, "BOOKING_CREATED", "BOOKING",
			UUID.randomUUID(), Map.of("sessionId", "s1"), false, null, Instant.parse("2026-10-12T00:00:00Z"));
		when(service.list(eq(accountId), eq(true), any(Pageable.class)))
			.thenReturn(new NotificationPageResponse(List.of(response), 0, 20, 1, 1, 1));

		mockMvc.perform(get("/api/v1/notifications").contextPath("/api/v1").with(jwt().jwt(jwt ->
			jwt.subject(accountId.toString()))).queryParam("unreadOnly", "true"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[0].id").value(notificationId.toString()))
			.andExpect(jsonPath("$.unreadCount").value(1));
	}

	@Test
	void rejectsInvalidPagination() throws Exception {
		UUID accountId = UUID.randomUUID();
		mockMvc.perform(get("/api/v1/notifications").contextPath("/api/v1")
			.with(jwt().jwt(jwt -> jwt.subject(accountId.toString())))
			.queryParam("page", "-1"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
	}

	@Test
	void marksReadAndAllRead() throws Exception {
		UUID accountId = UUID.randomUUID();
		UUID notificationId = UUID.randomUUID();
		when(service.markRead(accountId, notificationId)).thenReturn(new NotificationReadResponse(1, 0));
		when(service.markAllRead(accountId)).thenReturn(new NotificationReadResponse(2, 0));

		mockMvc.perform(patch("/api/v1/notifications/{id}/read", notificationId).contextPath("/api/v1")
			.with(jwt().jwt(jwt -> jwt.subject(accountId.toString()))))
			.andExpect(status().isOk()).andExpect(jsonPath("$.updatedCount").value(1));
		mockMvc.perform(patch("/api/v1/notifications/read-all").contextPath("/api/v1")
			.with(jwt().jwt(jwt -> jwt.subject(accountId.toString()))))
			.andExpect(status().isOk()).andExpect(jsonPath("$.updatedCount").value(2));
	}
}
