package com.scms.backend.scheduling;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "room_suspensions")
public class RoomSuspension {

	@Id
	@Column(nullable = false, updatable = false)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "room_id", nullable = false, updatable = false)
	private Room room;

	@Column(name = "start_time", nullable = false, updatable = false)
	private Instant startTime;

	@Column(name = "end_time", nullable = false, updatable = false)
	private Instant endTime;

	@Column(length = 1000, updatable = false)
	private String reason;

	@Column(name = "created_by_account_id", nullable = false, updatable = false)
	private UUID createdBy;

	@Column(name = "created_at", nullable = false, insertable = false, updatable = false)
	private Instant createdAt;

	protected RoomSuspension() {
	}

	RoomSuspension(UUID id, Room room, Instant startTime, Instant endTime, String reason, UUID createdBy) {
		this.id = id;
		this.room = room;
		this.startTime = startTime;
		this.endTime = endTime;
		this.reason = reason;
		this.createdBy = createdBy;
	}

	UUID getId() { return id; }
	Room getRoom() { return room; }
	Instant getStartTime() { return startTime; }
	Instant getEndTime() { return endTime; }
	String getReason() { return reason; }
	UUID getCreatedBy() { return createdBy; }
}
