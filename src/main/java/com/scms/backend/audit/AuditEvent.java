package com.scms.backend.audit;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.generator.EventType;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "audit_events")
public class AuditEvent {

	@Id
	@Column(nullable = false, updatable = false)
	private UUID id;

	@Column(name = "actor_account_id")
	private UUID actorAccountId;

	@Column(nullable = false, length = 120, updatable = false)
	private String action;

	@Column(name = "target_type", nullable = false, length = 80, updatable = false)
	private String targetType;

	@Column(name = "target_id", updatable = false)
	private UUID targetId;

	@Column(columnDefinition = "text", updatable = false)
	private String reason;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "before_data", columnDefinition = "jsonb", updatable = false)
	private Map<String, Object> beforeData;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "after_data", columnDefinition = "jsonb", updatable = false)
	private Map<String, Object> afterData;

	@Generated(event = EventType.INSERT)
	@Column(name = "created_at", nullable = false, insertable = false, updatable = false)
	private Instant createdAt;

	protected AuditEvent() {
	}

	public AuditEvent(UUID id, String action, String targetType, UUID targetId, Map<String, Object> afterData) {
		this.id = Objects.requireNonNull(id);
		this.action = Objects.requireNonNull(action);
		this.targetType = Objects.requireNonNull(targetType);
		this.targetId = Objects.requireNonNull(targetId);
		this.afterData = Map.copyOf(Objects.requireNonNull(afterData));
	}

	public UUID getId() {
		return id;
	}

	public UUID getActorAccountId() {
		return actorAccountId;
	}

	public String getAction() {
		return action;
	}

	public String getTargetType() {
		return targetType;
	}

	public UUID getTargetId() {
		return targetId;
	}

	public String getReason() {
		return reason;
	}

	public Map<String, Object> getBeforeData() {
		return beforeData;
	}

	public Map<String, Object> getAfterData() {
		return afterData;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
