package com.scms.backend.notification;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class NotificationWriterTests {

	@Test
	void writesPendingNotificationWithStableEventKeyAndJsonPayload() {
		NotificationRepository repository = mock(NotificationRepository.class);
		NotificationWriter writer = new NotificationWriter(repository);
		UUID recipientId = UUID.randomUUID();
		UUID bookingId = UUID.randomUUID();

		writer.write("BOOKING_CREATED:" + bookingId, recipientId, "BOOKING_CREATED", "BOOKING", bookingId,
			Map.of("sessionId", "session-1"));

		verify(repository).insertPending(any(UUID.class), eq("BOOKING_CREATED:" + bookingId), eq(recipientId),
			eq("BOOKING_CREATED"), eq("BOOKING"), eq(bookingId), eq("{\"sessionId\":\"session-1\"}"));
	}
}
