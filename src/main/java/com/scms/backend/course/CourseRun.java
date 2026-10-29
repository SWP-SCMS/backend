package com.scms.backend.course;

import java.math.BigInteger;
import java.time.Instant;
import java.util.UUID;

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

@Entity
@Table(name = "course_runs")
public class CourseRun {

	@Id
	@Column(nullable = false, updatable = false)
	private UUID id;

	@Column(name = "run_code", nullable = false, length = 50, unique = true, updatable = false)
	private String runCode;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "course_id", nullable = false, updatable = false)
	private Course course;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "primary_coach_account_id", nullable = false, updatable = false)
	private Account primaryCoach;

	@Column(name = "tuition_vnd", nullable = false, precision = 19, scale = 0, updatable = false)
	private BigInteger tuitionVnd;

	@Column(nullable = false, updatable = false)
	private int capacity;

	@Column(name = "minimum_enrollment", nullable = false)
	private int minimumEnrollment;

	@Column(name = "registration_opens_at", nullable = false, updatable = false)
	private Instant registrationOpensAt;

	@Column(name = "registration_closes_at", nullable = false, updatable = false)
	private Instant registrationClosesAt;

	@Column(name = "planned_start_at", nullable = false, updatable = false)
	private Instant plannedStartAt;

	@Column(name = "planned_end_at", nullable = false, updatable = false)
	private Instant plannedEndAt;

	@Column(name = "total_units", nullable = false, updatable = false)
	private int totalUnits;

	@Column(name = "named_coach_commitment", nullable = false, updatable = false)
	private boolean namedCoachCommitment;

	@Column(name = "policy_version_snapshot", length = 100)
	private String policyVersionSnapshot;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private CourseRunStatus status;

	@Column(name = "created_at", nullable = false, insertable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false, insertable = false)
	private Instant updatedAt;

	@Version
	@Column(nullable = false)
	private Long version;

	protected CourseRun() {
	}

	CourseRun(UUID id, String runCode, Course course, Account primaryCoach, BigInteger tuitionVnd,
			int capacity, int minimumEnrollment, Instant registrationOpensAt, Instant registrationClosesAt,
			Instant plannedStartAt, Instant plannedEndAt, int totalUnits, boolean namedCoachCommitment) {
		this.id = id;
		this.runCode = runCode;
		this.course = course;
		this.primaryCoach = primaryCoach;
		this.tuitionVnd = tuitionVnd;
		this.capacity = capacity;
		this.minimumEnrollment = minimumEnrollment;
		this.registrationOpensAt = registrationOpensAt;
		this.registrationClosesAt = registrationClosesAt;
		this.plannedStartAt = plannedStartAt;
		this.plannedEndAt = plannedEndAt;
		this.totalUnits = totalUnits;
		this.namedCoachCommitment = namedCoachCommitment;
		this.status = CourseRunStatus.DRAFT;
	}

	void openForRegistration(String policyVersion) {
		if (status != CourseRunStatus.DRAFT) throw CourseException.invalidTransition();
		if (!hasValidOpeningDetails(policyVersion)) throw CourseException.invalidOpening();
		policyVersionSnapshot = policyVersion;
		status = CourseRunStatus.OPEN_FOR_REGISTRATION;
	}

	void confirm() {
		transition(CourseRunStatus.OPEN_FOR_REGISTRATION, CourseRunStatus.CONFIRMED);
	}

	void start() {
		transition(CourseRunStatus.CONFIRMED, CourseRunStatus.IN_PROGRESS);
	}

	void markSettlementPending() {
		transition(CourseRunStatus.IN_PROGRESS, CourseRunStatus.SETTLEMENT_PENDING);
	}

	void complete() {
		transition(CourseRunStatus.SETTLEMENT_PENDING, CourseRunStatus.COMPLETED);
	}

	void cancel() {
		if (status != CourseRunStatus.DRAFT && status != CourseRunStatus.OPEN_FOR_REGISTRATION
				&& status != CourseRunStatus.CONFIRMED && status != CourseRunStatus.IN_PROGRESS) {
			throw CourseException.invalidTransition();
		}
		status = CourseRunStatus.CANCELLED;
	}

	void changeMinimumEnrollment(int minimumEnrollment) {
		if (status != CourseRunStatus.DRAFT) throw CourseException.immutableRun();
		if (minimumEnrollment <= 0 || minimumEnrollment > capacity) throw CourseException.invalidOpening();
		this.minimumEnrollment = minimumEnrollment;
	}

	CourseRunStatus getStatus() {
		return status;
	}

	String getPolicyVersionSnapshot() {
		return policyVersionSnapshot;
	}

	int getMinimumEnrollment() {
		return minimumEnrollment;
	}

	private boolean hasValidOpeningDetails(String policyVersion) {
		return tuitionVnd != null && tuitionVnd.signum() > 0
			&& capacity > 0 && minimumEnrollment > 0 && minimumEnrollment <= capacity
			&& registrationOpensAt != null && registrationClosesAt != null
			&& plannedStartAt != null && plannedEndAt != null
			&& registrationOpensAt.isBefore(registrationClosesAt)
			&& !registrationClosesAt.isAfter(plannedStartAt)
			&& plannedStartAt.isBefore(plannedEndAt)
			&& totalUnits > 0 && policyVersion != null && !policyVersion.isBlank();
	}

	private void transition(CourseRunStatus required, CourseRunStatus next) {
		if (status != required) throw CourseException.invalidTransition();
		status = next;
	}
}
