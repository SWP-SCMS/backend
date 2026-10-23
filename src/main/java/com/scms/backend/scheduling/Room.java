package com.scms.backend.scheduling;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "rooms")
public class Room {

	@Id
	@Column(nullable = false, updatable = false)
	private UUID id;

	@Column(nullable = false, length = 150)
	private String name;

	@Column(nullable = false)
	private int capacity;

	@Enumerated(EnumType.STRING)
	@Column(name = "usage_mode", nullable = false, length = 20)
	private RoomUsageMode usageMode;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private RoomStatus status;

	@Column(name = "created_at", nullable = false, insertable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false, insertable = false)
	private Instant updatedAt;

	@Version
	@Column(nullable = false)
	private Long version;

	protected Room() {
	}

	Room(UUID id, String name, int capacity, RoomStatus status) {
		this(id, name, capacity, status, RoomUsageMode.EXCLUSIVE);
	}

	Room(UUID id, String name, int capacity, RoomStatus status, RoomUsageMode usageMode) {
		this.id = id;
		this.name = name;
		this.capacity = capacity;
		this.status = status;
		this.usageMode = usageMode;
	}

	void update(String name, int capacity, RoomStatus status) {
		update(name, capacity, status, usageMode);
	}

	void update(String name, int capacity, RoomStatus status, RoomUsageMode usageMode) {
		this.name = name;
		this.capacity = capacity;
		this.status = status;
		this.usageMode = usageMode;
	}

	UUID getId() { return id; }
	String getName() { return name; }
	int getCapacity() { return capacity; }
	RoomStatus getStatus() { return status; }
	RoomUsageMode getUsageMode() { return usageMode; }
}
