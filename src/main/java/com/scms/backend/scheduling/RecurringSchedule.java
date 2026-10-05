package com.scms.backend.scheduling;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "recurring_schedules")
public class RecurringSchedule {

	@Id
	@Column(nullable = false, updatable = false)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "sport_class_id", nullable = false, updatable = false)
	private SportClass sportClass;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "teaching_coach_account_id", nullable = false, updatable = false)
	private com.scms.backend.account.Account teachingCoach;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "room_id", nullable = false, updatable = false)
	private Room room;

	@Column(name = "start_date", nullable = false, updatable = false)
	private LocalDate startDate;

	@JdbcTypeCode(SqlTypes.ARRAY)
	@Column(nullable = false, columnDefinition = "smallint[]", updatable = false)
	private Short[] weekdays;

	@Column(name = "start_time", nullable = false, updatable = false)
	private LocalTime startTime;

	@Column(name = "end_time", nullable = false, updatable = false)
	private LocalTime endTime;

	@Column(name = "session_capacity", nullable = false, updatable = false)
	private int sessionCapacity;

	@Column(name = "session_count", nullable = false, updatable = false)
	private short sessionCount = 30;

	@Column(name = "created_by_account_id", nullable = false, updatable = false)
	private UUID createdByAccountId;

	@Column(name = "created_at", nullable = false, insertable = false, updatable = false)
	private Instant createdAt;

	protected RecurringSchedule() {
	}

	RecurringSchedule(UUID id, SportClass sportClass, com.scms.backend.account.Account teachingCoach, Room room,
			LocalDate startDate, Short[] weekdays, LocalTime startTime, LocalTime endTime,
			int sessionCapacity, UUID createdByAccountId) {
		this.id = id;
		this.sportClass = sportClass;
		this.teachingCoach = teachingCoach;
		this.room = room;
		this.startDate = startDate;
		this.weekdays = weekdays;
		this.startTime = startTime;
		this.endTime = endTime;
		this.sessionCapacity = sessionCapacity;
		this.createdByAccountId = createdByAccountId;
	}

	UUID getId() { return id; }
	short getSessionCount() { return sessionCount; }
}
