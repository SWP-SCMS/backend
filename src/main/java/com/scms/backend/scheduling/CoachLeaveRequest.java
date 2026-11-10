package com.scms.backend.scheduling;

import com.scms.backend.account.Account;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "coach_leave_requests")
public class CoachLeaveRequest {

	@Id
	@Column(nullable = false, updatable = false)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "coach_account_id", nullable = false, updatable = false)
	private Account coach;

	@Column(name = "start_time", nullable = false, updatable = false)
	private Instant startTime;

	@Column(name = "end_time", nullable = false, updatable = false)
	private Instant endTime;

	@Column(nullable = false, length = 1000, updatable = false)
	private String reason;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private CoachLeaveStatus status;

	@Column(name = "reviewed_by_account_id")
	private UUID reviewedBy;

	@Column(name = "review_note", length = 1000)
	private String reviewNote;

	@Column(name = "reviewed_at")
	private Instant reviewedAt;

	@Column(name = "created_at", nullable = false, insertable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false, insertable = false)
	private Instant updatedAt;

	@Version
	@Column(nullable = false)
	private Long version;

	protected CoachLeaveRequest() {
	}

	public CoachLeaveRequest(UUID id, Account coach, Instant startTime, Instant endTime, String reason) {
		this.id = id;
		this.coach = coach;
		this.startTime = startTime;
		this.endTime = endTime;
		this.reason = reason;
		this.status = CoachLeaveStatus.PENDING;
	}

	public UUID getId() {
		return id;
	}

	public Account getCoach() {
		return coach;
	}

	public Instant getStartTime() {
		return startTime;
	}

	public Instant getEndTime() {
		return endTime;
	}

	public String getReason() {
		return reason;
	}

	public CoachLeaveStatus getStatus() {
		return status;
	}

	public UUID getReviewedBy() {
		return reviewedBy;
	}

	public String getReviewNote() {
		return reviewNote;
	}

	public Instant getReviewedAt() {
		return reviewedAt;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void approve(UUID managerId, String note, Instant now) {
		review(CoachLeaveStatus.APPROVED, managerId, note, now);
	}

	public void reject(UUID managerId, String note, Instant now) {
		review(CoachLeaveStatus.REJECTED, managerId, note, now);
	}

	public void withdraw() {
		status = CoachLeaveStatus.WITHDRAWN;
	}

	public void cancel(Instant now) {
		if (status != CoachLeaveStatus.APPROVED || !now.isBefore(startTime)) {
			throw new IllegalStateException("Only a future approved leave request can be cancelled");
		}
		status = CoachLeaveStatus.CANCELLED;
	}

	public void expire(Instant now) {
		if (status != CoachLeaveStatus.PENDING || startTime.isAfter(now)) {
			throw new IllegalStateException("Only a started pending leave request can expire");
		}
		status = CoachLeaveStatus.EXPIRED;
	}

	private void review(CoachLeaveStatus target, UUID managerId, String note, Instant now) {
		status = target;
		reviewedBy = managerId;
		reviewNote = note;
		reviewedAt = now;
	}
}
