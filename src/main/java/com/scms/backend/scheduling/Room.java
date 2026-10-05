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
		this.id = id;
		this.name = name;
		this.capacity = capacity;
		this.status = status;
	}

	void update(String name, int capacity, RoomStatus status) {
		this.name = name;
		this.capacity = capacity;
		this.status = status;
	}

	UUID getId() { return id; }
	String getName() { return name; }
	int getCapacity() { return capacity; }
	RoomStatus getStatus() { return status; }
}
