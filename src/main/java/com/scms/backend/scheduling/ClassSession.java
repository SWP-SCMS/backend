package com.scms.backend.scheduling;

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
@Table(name = "class_sessions")
public class ClassSession {

	@Id
	@Column(nullable = false, updatable = false)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "sport_class_id", nullable = false, updatable = false)
	private SportClass sportClass;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "recurring_schedule_id", updatable = false)
	private RecurringSchedule recurringSchedule;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "teaching_coach_account_id", nullable = false)
	private Account teachingCoach;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "room_id", nullable = false)
	private Room room;

	@Column(name = "start_time", nullable = false, updatable = false)
	private Instant startTime;

	@Column(name = "end_time", nullable = false, updatable = false)
	private Instant endTime;

	@Column(nullable = false, updatable = false)
	private int capacity;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ClassSessionStatus status;

	@Column(name = "cancelled_at")
	private Instant cancelledAt;

	@Column(name = "cancellation_reason", columnDefinition = "text")
	private String cancellationReason;

	@Column(name = "created_by_account_id", nullable = false, updatable = false)
	private UUID createdBy;

	@Column(name = "created_at", nullable = false, insertable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false, insertable = false)
	private Instant updatedAt;

	@Version
	@Column(nullable = false)
	private Long version;

	protected ClassSession() {
	}

	ClassSession(UUID id, SportClass sportClass, RecurringSchedule recurringSchedule, Account teachingCoach,
			Room room, Instant startTime, Instant endTime, int capacity, UUID createdBy) {
		this.id = id;
		this.sportClass = sportClass;
		this.recurringSchedule = recurringSchedule;
		this.teachingCoach = teachingCoach;
		this.room = room;
		this.startTime = startTime;
		this.endTime = endTime;
		this.capacity = capacity;
		this.status = ClassSessionStatus.SCHEDULED;
		this.createdBy = createdBy;
	}

	UUID getId() { return id; }
	SportClass getSportClass() { return sportClass; }
	RecurringSchedule getRecurringSchedule() { return recurringSchedule; }
	Account getTeachingCoach() { return teachingCoach; }
	Room getRoom() { return room; }
	Instant getStartTime() { return startTime; }
	Instant getEndTime() { return endTime; }
	int getCapacity() { return capacity; }
	ClassSessionStatus getStatus() { return status; }
	Instant getCancelledAt() { return cancelledAt; }
	String getCancellationReason() { return cancellationReason; }
	UUID getCreatedBy() { return createdBy; }

	void updateAssignment(Account coach, Room room) {
		this.teachingCoach = coach;
		this.room = room;
	}

	void cancel(Instant cancelledAt, String reason) {
		this.status = ClassSessionStatus.CANCELLED;
		this.cancelledAt = cancelledAt;
		this.cancellationReason = reason;
	}
}
