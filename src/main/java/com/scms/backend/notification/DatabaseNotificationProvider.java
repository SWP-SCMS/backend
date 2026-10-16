package com.scms.backend.notification;

import java.util.UUID;

import org.springframework.stereotype.Service;

@Service
class DatabaseNotificationProvider implements NotificationProvider {

	private final NotificationRepository notifications;

	DatabaseNotificationProvider(NotificationRepository notifications) {
		this.notifications = notifications;
	}

	@Override
	public void deliver(NotificationOutboxEvent event) {
		notifications.insertPending(UUID.randomUUID(), event.eventKey(), event.recipientId(), event.notificationType(),
			event.targetType(), event.targetId(), event.payload());
	}
}
