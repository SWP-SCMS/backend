package com.scms.backend.scheduling;

import java.time.Instant;
import java.util.UUID;

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
@Table(name = "sport_classes")
public class SportClass {

	@Id
	@Column(nullable = false, updatable = false)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "discipline_id", nullable = false, updatable = false)
	private Discipline discipline;

	@Column(nullable = false, length = 150)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(name = "class_type", nullable = false, length = 20)
	private SportClassType classType;

	@Column(columnDefinition = "text")
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private SportClassStatus status;

	@Column(name = "created_at", nullable = false, insertable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false, insertable = false)
	private Instant updatedAt;

	@Version
	@Column(nullable = false)
	private Long version;

	protected SportClass() {
	}

	SportClass(UUID id, Discipline discipline, String name, SportClassType classType,
			String description, SportClassStatus status) {
		this.id = id;
		this.discipline = discipline;
		this.name = name;
		this.classType = classType;
		this.description = description;
		this.status = status;
	}

	void update(String name, SportClassType classType, String description, SportClassStatus status) {
		this.name = name;
		this.classType = classType;
		this.description = description;
		this.status = status;
	}

	UUID getId() { return id; }
	Discipline getDiscipline() { return discipline; }
	String getName() { return name; }
	SportClassType getClassType() { return classType; }
	String getDescription() { return description; }
	SportClassStatus getStatus() { return status; }
}
