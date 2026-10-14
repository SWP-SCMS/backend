package com.scms.backend.training;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "feedback")
public class TrainingFeedback {

	@Id
	@Column(nullable = false, updatable = false)
	private UUID id;

	@Column(name = "member_account_id", nullable = false, updatable = false)
	private UUID memberAccountId;

	@Column(name = "coach_assignment_id", updatable = false)
	private UUID coachAssignmentId;

	@Column(name = "training_plan_version_id")
	private UUID trainingPlanVersionId;

	@Column(name = "training_result_id")
	private UUID trainingResultId;

	@Column(nullable = false, columnDefinition = "text")
	private String content;

	@Column(name = "created_by_account_id", nullable = false, updatable = false)
	private UUID createdByAccountId;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Version
	@Column(nullable = false)
	private Long version;

	protected TrainingFeedback() {
	}

	public TrainingFeedback(UUID id, UUID memberAccountId, UUID coachAssignmentId,
			UUID trainingPlanVersionId, UUID trainingResultId, String content,
			UUID createdByAccountId, Instant createdAt) {
		this.id = Objects.requireNonNull(id);
		this.memberAccountId = Objects.requireNonNull(memberAccountId);
		this.coachAssignmentId = Objects.requireNonNull(coachAssignmentId);
		setContext(trainingPlanVersionId, trainingResultId);
		this.content = requireContent(content);
		this.createdByAccountId = Objects.requireNonNull(createdByAccountId);
		this.createdAt = Objects.requireNonNull(createdAt);
		this.updatedAt = createdAt;
	}

	public void update(UUID trainingPlanVersionId, UUID trainingResultId, String content, Instant updatedAt) {
		setContext(trainingPlanVersionId, trainingResultId);
		this.content = requireContent(content);
		this.updatedAt = Objects.requireNonNull(updatedAt);
	}

	private void setContext(UUID trainingPlanVersionId, UUID trainingResultId) {
		if ((trainingPlanVersionId == null) == (trainingResultId == null)) {
			throw new IllegalArgumentException("Feedback requires exactly one Training context.");
		}
		this.trainingPlanVersionId = trainingPlanVersionId;
		this.trainingResultId = trainingResultId;
	}

	private String requireContent(String value) {
		String normalized = Objects.requireNonNull(value).trim();
		if (normalized.isEmpty()) throw new IllegalArgumentException("Feedback content must not be blank.");
		return normalized;
	}

	public UUID getId() { return id; }
	public UUID getMemberAccountId() { return memberAccountId; }
	public UUID getCoachAssignmentId() { return coachAssignmentId; }
	public UUID getTrainingPlanVersionId() { return trainingPlanVersionId; }
	public UUID getTrainingResultId() { return trainingResultId; }
	public String getContent() { return content; }
	public UUID getCreatedByAccountId() { return createdByAccountId; }
	public Instant getCreatedAt() { return createdAt; }
	public Instant getUpdatedAt() { return updatedAt; }
	public Long getVersion() { return version; }
}
