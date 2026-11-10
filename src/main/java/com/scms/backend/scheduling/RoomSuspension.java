package com.scms.backend.scheduling;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

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

	@Column(name = "end_time", nullable = false)
	private Instant endTime;

	@Column(length = 1000)
	private String reason;

	@Column(name = "created_by_account_id", nullable = false, updatable = false)
	private UUID createdBy;

	@Column(name = "created_at", nullable = false, insertable = false, updatable = false)
	private Instant createdAt;

	protected RoomSuspension() {
	}

	public RoomSuspension(UUID id, Room room, Instant startTime, Instant endTime, String reason, UUID createdBy) {
		this.id = id;
		this.room = room;
		this.startTime = startTime;
		this.endTime = endTime;
		this.reason = reason;
		this.createdBy = createdBy;
	}

	public UUID getId() {
		return id;
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

	public String getReason() {
		return reason;
	}

	public UUID getCreatedBy() {
		return createdBy;
	}

	public void extend(Instant endTime, String reason) {
		this.endTime = endTime;
		this.reason = reason;
	}
}
