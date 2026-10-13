package com.scms.backend.training;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "training_plans")
public class TrainingPlan {

	@Id
	@Column(nullable = false, updatable = false)
	private UUID id;

	@Column(name = "member_account_id", nullable = false, updatable = false)
	private UUID memberAccountId;

	@Column(name = "coach_assignment_id", nullable = false, updatable = false)
	private UUID coachAssignmentId;

	@Column(nullable = false, length = 200)
	private String title;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private TrainingPlanStatus status;

	@Column(name = "created_by_account_id", nullable = false, updatable = false)
	private UUID createdByAccountId;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Version
	@Column(nullable = false)
	private Long version;

	protected TrainingPlan() {
	}

	public TrainingPlan(UUID id, UUID memberAccountId, UUID coachAssignmentId, String title,
			UUID createdByAccountId, Instant createdAt) {
		this.id = Objects.requireNonNull(id);
		this.memberAccountId = Objects.requireNonNull(memberAccountId);
		this.coachAssignmentId = Objects.requireNonNull(coachAssignmentId);
		this.title = Objects.requireNonNull(title);
		this.status = TrainingPlanStatus.ACTIVE;
		this.createdByAccountId = Objects.requireNonNull(createdByAccountId);
		this.createdAt = Objects.requireNonNull(createdAt);
		this.updatedAt = createdAt;
	}

	public void update(String title, Instant updatedAt) {
		this.title = Objects.requireNonNull(title);
		this.updatedAt = Objects.requireNonNull(updatedAt);
	}

	public UUID getId() { return id; }
	public UUID getMemberAccountId() { return memberAccountId; }
	public UUID getCoachAssignmentId() { return coachAssignmentId; }
	public String getTitle() { return title; }
	public TrainingPlanStatus getStatus() { return status; }
	public UUID getCreatedByAccountId() { return createdByAccountId; }
	public Instant getCreatedAt() { return createdAt; }
	public Instant getUpdatedAt() { return updatedAt; }
	public Long getVersion() { return version; }
}
