package com.scms.backend.membership;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.UUID;

import com.scms.backend.notification.NotificationRepository;
import org.junit.jupiter.api.Test;

class PaymentNotificationListenerTests {
	@Test
	void notificationFailureIsContainedAfterPaymentCommit() {
		NotificationRepository notifications = mock(NotificationRepository.class);
		when(notifications.save(any())).thenThrow(new IllegalStateException("notification unavailable"));
		PaymentNotificationListener listener = new PaymentNotificationListener(notifications);

		assertThatCode(() -> listener.notifyMember(new PaymentCompletedEvent(
			UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()))).doesNotThrowAnyException();
	}
}
