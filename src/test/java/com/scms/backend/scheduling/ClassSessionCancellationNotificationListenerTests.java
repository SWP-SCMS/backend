package com.scms.backend.scheduling;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import com.scms.backend.notification.NotificationRepository;
import org.junit.jupiter.api.Test;

class ClassSessionCancellationNotificationListenerTests {

	@Test
	void notificationFailureIsContainedAfterCancellationCommit() {
		NotificationRepository notifications = mock(NotificationRepository.class);
		when(notifications.saveAll(anyList())).thenThrow(new IllegalStateException("notification unavailable"));
		ClassSessionCancellationNotificationListener listener =
			new ClassSessionCancellationNotificationListener(notifications);

		assertThatCode(() -> listener.notifyMembers(new ClassSessionCancelledEvent(UUID.randomUUID(),
			List.of(UUID.randomUUID())))).doesNotThrowAnyException();
	}
}
