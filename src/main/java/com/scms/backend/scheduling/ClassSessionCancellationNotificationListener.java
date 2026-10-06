package com.scms.backend.scheduling;

import java.util.Map;
import java.util.UUID;

import com.scms.backend.notification.Notification;
import com.scms.backend.notification.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
class ClassSessionCancellationNotificationListener {
	private static final Logger logger = LoggerFactory.getLogger(ClassSessionCancellationNotificationListener.class);

	private final NotificationRepository notifications;

	ClassSessionCancellationNotificationListener(NotificationRepository notifications) {
		this.notifications = notifications;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	void notifyMembers(ClassSessionCancelledEvent event) {
		try {
			notifications.saveAll(event.memberIds().stream()
				.map(memberId -> new Notification(UUID.randomUUID(), memberId, "SESSION_CANCELLED", "CLASS_SESSION",
					event.sessionId(), Map.of("sessionId", event.sessionId().toString())))
				.toList());
		}
		catch (RuntimeException exception) {
			logger.error("Could not create cancellation notifications for Session {}", event.sessionId(), exception);
		}
	}
}
