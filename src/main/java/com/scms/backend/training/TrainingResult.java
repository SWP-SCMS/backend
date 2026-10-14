package com.scms.backend.training;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "training_results")
public class TrainingResult {

	@Id
	@Column(nullable = false, updatable = false)
	private UUID id;

	@Column(name = "member_account_id", nullable = false, updatable = false)
	private UUID memberAccountId;

	@Column(name = "coach_assignment_id", nullable = false, updatable = false)
	private UUID coachAssignmentId;

	@Column(name = "training_plan_version_id")
	private UUID trainingPlanVersionId;

	@Column(name = "class_session_id", updatable = false)
	private UUID classSessionId;

	@Column(name = "activity_at", nullable = false)
	private Instant activityAt;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(nullable = false, columnDefinition = "jsonb")
	private Map<String, Object> content;

	@Column(name = "created_by_account_id", nullable = false, updatable = false)
	private UUID createdByAccountId;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Version
	@Column(nullable = false)
	private Long version;

	protected TrainingResult() {
	}

	public TrainingResult(UUID id, UUID memberAccountId, UUID coachAssignmentId,
			UUID trainingPlanVersionId, Instant activityAt, Map<String, Object> content,
			UUID createdByAccountId, Instant createdAt) {
		this.id = Objects.requireNonNull(id);
		this.memberAccountId = Objects.requireNonNull(memberAccountId);
		this.coachAssignmentId = Objects.requireNonNull(coachAssignmentId);
		this.trainingPlanVersionId = Objects.requireNonNull(trainingPlanVersionId);
		this.activityAt = Objects.requireNonNull(activityAt);
		this.content = new LinkedHashMap<>(Objects.requireNonNull(content));
		this.createdByAccountId = Objects.requireNonNull(createdByAccountId);
		this.createdAt = Objects.requireNonNull(createdAt);
		this.updatedAt = createdAt;
	}

	public void update(UUID trainingPlanVersionId, Instant activityAt, Map<String, Object> content,
			Instant updatedAt) {
		this.trainingPlanVersionId = Objects.requireNonNull(trainingPlanVersionId);
		this.activityAt = Objects.requireNonNull(activityAt);
		this.content = new LinkedHashMap<>(Objects.requireNonNull(content));
		this.updatedAt = Objects.requireNonNull(updatedAt);
	}

	public UUID getId() { return id; }
	public UUID getMemberAccountId() { return memberAccountId; }
	public UUID getCoachAssignmentId() { return coachAssignmentId; }
	public UUID getTrainingPlanVersionId() { return trainingPlanVersionId; }
	public UUID getClassSessionId() { return classSessionId; }
	public Instant getActivityAt() { return activityAt; }
	public Map<String, Object> getContent() {
		return Collections.unmodifiableMap(new LinkedHashMap<>(content));
	}
	public UUID getCreatedByAccountId() { return createdByAccountId; }
	public Instant getCreatedAt() { return createdAt; }
	public Instant getUpdatedAt() { return updatedAt; }
	public Long getVersion() { return version; }
}
