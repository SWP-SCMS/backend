package com.scms.backend.course;

import java.math.BigInteger;
import java.time.Instant;
import java.util.UUID;

import com.scms.backend.account.Account;
import com.scms.backend.scheduling.Room;
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
@Table(name = "course_sessions")
public class CourseSession {

	@Id
	@Column(nullable = false, updatable = false)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "course_run_id", nullable = false, updatable = false)
	private CourseRun courseRun;

	@Column(name = "unit_number", nullable = false, updatable = false)
	private int unitNumber;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "room_id", nullable = false)
	private Room room;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "coach_account_id", nullable = false)
	private Account coach;

	@Column(name = "start_at", nullable = false)
	private Instant startAt;

	@Column(name = "end_at", nullable = false)
	private Instant endAt;

	@Column(name = "original_start_at", nullable = false, updatable = false)
	private Instant originalStartAt;

	@Column(name = "original_end_at", nullable = false, updatable = false)
	private Instant originalEndAt;

	@Column(name = "allocation_vnd", nullable = false, precision = 19, scale = 0)
	private BigInteger allocationVnd;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private CourseSessionStatus status;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "replacement_session_id")
	private CourseSession replacementSession;

	@Column(name = "created_at", nullable = false, insertable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false, insertable = false)
	private Instant updatedAt;

	@Version
	@Column(nullable = false)
	private Long version;

	protected CourseSession() {
	}

	CourseSession(UUID id, CourseRun courseRun, int unitNumber, Room room, Account coach, Instant startAt,
			Instant endAt, BigInteger allocationVnd) {
		this.id = id;
		this.courseRun = courseRun;
		this.unitNumber = unitNumber;
		this.room = room;
		this.coach = coach;
		this.startAt = startAt;
		this.endAt = endAt;
		this.originalStartAt = startAt;
		this.originalEndAt = endAt;
		this.allocationVnd = allocationVnd;
		this.status = CourseSessionStatus.SCHEDULED;
	}
}
