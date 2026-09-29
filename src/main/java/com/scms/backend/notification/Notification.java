package com.scms.backend.notification;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.generator.EventType;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "notifications")
public class Notification {

	@Id
	@Column(nullable = false, updatable = false)
	private UUID id;

	@Column(name = "recipient_account_id", nullable = false, updatable = false)
	private UUID recipientAccountId;

	@Column(name = "notification_type", nullable = false, length = 80, updatable = false)
	private String notificationType;

	@Column(name = "target_type", nullable = false, length = 80, updatable = false)
	private String targetType;

	@Column(name = "target_id", updatable = false)
	private UUID targetId;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(nullable = false, columnDefinition = "jsonb", updatable = false)
	private Map<String, Object> payload;

	@Enumerated(EnumType.STRING)
	@Column(name = "delivery_status", nullable = false, length = 20)
	private NotificationDeliveryStatus deliveryStatus;

	@Column(name = "delivery_attempt_count", nullable = false)
	private int deliveryAttemptCount;

	@Column(name = "last_delivery_attempt_at")
	private Instant lastDeliveryAttemptAt;

	@Column(name = "delivered_at")
	private Instant deliveredAt;

	@Column(name = "read_at")
	private Instant readAt;

	@Generated(event = EventType.INSERT)
	@Column(name = "created_at", nullable = false, insertable = false, updatable = false)
	private Instant createdAt;

	protected Notification() {
	}

	public Notification(UUID id, UUID recipientAccountId, String notificationType, String targetType, UUID targetId,
			Map<String, Object> payload) {
		this.id = Objects.requireNonNull(id);
		this.recipientAccountId = Objects.requireNonNull(recipientAccountId);
		this.notificationType = Objects.requireNonNull(notificationType);
		this.targetType = Objects.requireNonNull(targetType);
		this.targetId = Objects.requireNonNull(targetId);
		this.payload = Map.copyOf(Objects.requireNonNull(payload));
		this.deliveryStatus = NotificationDeliveryStatus.PENDING;
	}

	public UUID getId() {
		return id;
	}

	public UUID getRecipientAccountId() {
		return recipientAccountId;
	}

	public String getNotificationType() {
		return notificationType;
	}

	public String getTargetType() {
		return targetType;
	}

	public UUID getTargetId() {
		return targetId;
	}

	public Map<String, Object> getPayload() {
		return payload;
	}

	public NotificationDeliveryStatus getDeliveryStatus() {
		return deliveryStatus;
	}

	public int getDeliveryAttemptCount() {
		return deliveryAttemptCount;
	}

	public Instant getLastDeliveryAttemptAt() {
		return lastDeliveryAttemptAt;
	}

	public Instant getDeliveredAt() {
		return deliveredAt;
	}

	public Instant getReadAt() {
		return readAt;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
