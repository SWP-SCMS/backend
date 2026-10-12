package com.scms.backend.notification;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record NotificationResponse(UUID id, String notificationType, String targetType, UUID targetId,
		Map<String, Object> payload, boolean read, Instant readAt, Instant createdAt) {

	static NotificationResponse from(Notification notification) {
		return new NotificationResponse(notification.getId(), notification.getNotificationType(),
			 notification.getTargetType(), notification.getTargetId(), notification.getPayload(), notification.getReadAt() != null,
			notification.getReadAt(),
			notification.getCreatedAt());
	}
}
