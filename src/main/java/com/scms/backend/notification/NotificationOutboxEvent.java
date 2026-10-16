package com.scms.backend.notification;

import java.util.UUID;

public record NotificationOutboxEvent(UUID id, String eventKey, UUID recipientId, String notificationType,
		String targetType, UUID targetId, String payload, int attemptCount) {
}
