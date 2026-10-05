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
@Table(name = "disciplines")
public class Discipline {

	@Id
	@Column(nullable = false, updatable = false)
	private UUID id;

	@Column(nullable = false, length = 150)
	private String name;

	@Column(columnDefinition = "text")
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private DisciplineStatus status;

	@Column(name = "created_at", nullable = false, insertable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false, insertable = false)
	private Instant updatedAt;

	@Version
	@Column(nullable = false)
	private Long version;

	protected Discipline() {
	}

	Discipline(UUID id, String name, String description, DisciplineStatus status) {
		this.id = id;
		this.name = name;
		this.description = description;
		this.status = status;
	}

	void update(String name, String description, DisciplineStatus status) {
		this.name = name;
		this.description = description;
		this.status = status;
	}

	UUID getId() {
		return id;
	}

	String getName() {
		return name;
	}

	String getDescription() {
		return description;
	}

	DisciplineStatus getStatus() {
		return status;
	}
}
