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
@Table(name = "coach_assignments")
public class CoachAssignment {

	@Id
	@Column(nullable = false, updatable = false)
	private UUID id;

	@Column(name = "member_account_id", nullable = false, updatable = false)
	private UUID memberAccountId;

	@Column(name = "coach_account_id", nullable = false, updatable = false)
	private UUID coachAccountId;

	@Column(name = "assigned_by_account_id", nullable = false, updatable = false)
	private UUID assignedByAccountId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private CoachAssignmentStatus status;

	@Column(name = "starts_at", nullable = false, updatable = false)
	private Instant startsAt;

	@Column(name = "ends_at")
	private Instant endsAt;

	@Column(name = "end_reason", columnDefinition = "text")
	private String endReason;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Version
	@Column(nullable = false)
	private Long version;

	protected CoachAssignment() {
	}

	public CoachAssignment(UUID id, UUID memberAccountId, UUID coachAccountId, UUID assignedByAccountId,
			CoachAssignmentStatus status, Instant startsAt, Instant createdAt) {
		this.id = Objects.requireNonNull(id);
		this.memberAccountId = Objects.requireNonNull(memberAccountId);
		this.coachAccountId = Objects.requireNonNull(coachAccountId);
		this.assignedByAccountId = Objects.requireNonNull(assignedByAccountId);
		this.status = Objects.requireNonNull(status);
		this.startsAt = Objects.requireNonNull(startsAt);
		this.createdAt = Objects.requireNonNull(createdAt);
		this.updatedAt = createdAt;
	}

	public void end(Instant endedAt, String reason) {
		this.status = CoachAssignmentStatus.ENDED;
		// Ensure ends_at >= starts_at to satisfy chk_coach_assignments_ends_at
		this.endsAt = endedAt.isBefore(this.startsAt) ? this.startsAt : endedAt;
		this.endReason = reason;
		this.updatedAt = endedAt;
	}

	public UUID getId() {
		return id;
	}

	public UUID getMemberAccountId() {
		return memberAccountId;
	}

	public UUID getCoachAccountId() {
		return coachAccountId;
	}

	public UUID getAssignedByAccountId() {
		return assignedByAccountId;
	}

	public CoachAssignmentStatus getStatus() {
		return status;
	}

	public Instant getStartsAt() {
		return startsAt;
	}

	public Instant getEndsAt() {
		return endsAt;
	}

	public String getEndReason() {
		return endReason;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public Long getVersion() {
		return version;
	}
}
