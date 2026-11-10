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

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "room_id")
	private Room room;

	@Column(name = "start_time", nullable = false)
	private Instant startTime;

	@Column(name = "end_time", nullable = false)
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

	public ClassSession(UUID id, SportClass sportClass, RecurringSchedule recurringSchedule, Account teachingCoach, Room room, Instant startTime, Instant endTime, int capacity, UUID createdBy) {
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

	public UUID getId() {
		return id;
	}

	public SportClass getSportClass() {
		return sportClass;
	}

	public RecurringSchedule getRecurringSchedule() {
		return recurringSchedule;
	}

	public Account getTeachingCoach() {
		return teachingCoach;
	}

	public Room getRoom() {
		return room;
	}

	public Instant getStartTime() {
		return startTime;
	}

	public Instant getEndTime() {
		return endTime;
	}

	public int getCapacity() {
		return capacity;
	}

	public ClassSessionStatus getStatus() {
		return status;
	}

	public Instant getCancelledAt() {
		return cancelledAt;
	}

	public String getCancellationReason() {
		return cancellationReason;
	}

	public UUID getCreatedBy() {
		return createdBy;
	}

	public Long getVersion() {
		return version;
	}

	public void updateAssignment(Account coach, Room room) {
		this.teachingCoach = coach;
		this.room = room;
	}

	public void updateSchedule(Instant startTime, Instant endTime) {
		this.startTime = startTime;
		this.endTime = endTime;
	}

	public void cancel(Instant cancelledAt, String reason) {
		this.status = ClassSessionStatus.CANCELLED;
		this.cancelledAt = cancelledAt;
		this.cancellationReason = reason;
	}
}
