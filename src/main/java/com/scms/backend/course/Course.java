package com.scms.backend.course;

import java.time.Instant;
import java.util.UUID;

import com.scms.backend.scheduling.Discipline;
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
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

@Entity
@Table(name = "courses")
public class Course {

	@Id
	@Column(nullable = false, updatable = false)
	private UUID id;

	@Column(nullable = false, length = 50, unique = true, updatable = false)
	private String code;

	@Column(nullable = false, length = 200)
	private String name;

	@Column(columnDefinition = "text")
	private String description;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "discipline_id", nullable = false, updatable = false)
	private Discipline discipline;

	@Column(name = "image_url", columnDefinition = "text")
	private String imageUrl;

	@Column(name = "default_minimum_enrollment")
	private Integer defaultMinimumEnrollment;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private CourseStatus status;

	@Column(name = "created_at", nullable = false, insertable = false, updatable = false)
	@Generated(event = EventType.INSERT)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false, insertable = false)
	@Generated(event = EventType.INSERT)
	private Instant updatedAt;

	@Version
	@Column(nullable = false)
	private Long version;

	protected Course() {
	}

	Course(UUID id, String code, String name, String description, Discipline discipline, String imageUrl,
			Integer defaultMinimumEnrollment) {
		this.id = id;
		this.code = code;
		this.name = name;
		this.description = description;
		this.discipline = discipline;
		this.imageUrl = imageUrl;
		this.defaultMinimumEnrollment = defaultMinimumEnrollment;
		this.status = CourseStatus.DRAFT;
	}

	void update(String name, String description, String imageUrl, Integer defaultMinimumEnrollment) {
		this.name = name;
		this.description = description;
		this.imageUrl = imageUrl;
		this.defaultMinimumEnrollment = defaultMinimumEnrollment;
		this.updatedAt = Instant.now();
	}

	void publish() {
		if (status != CourseStatus.DRAFT && status != CourseStatus.INACTIVE) {
			throw CourseException.invalidCourseTransition();
		}
		status = CourseStatus.PUBLISHED;
		updatedAt = Instant.now();
	}

	void deactivate() {
		if (status != CourseStatus.PUBLISHED) throw CourseException.invalidCourseTransition();
		status = CourseStatus.INACTIVE;
		updatedAt = Instant.now();
	}

	void archive() {
		if (status != CourseStatus.DRAFT && status != CourseStatus.INACTIVE) {
			throw CourseException.invalidCourseTransition();
		}
		status = CourseStatus.ARCHIVED;
		updatedAt = Instant.now();
	}

	public UUID getId() { return id; }
	public String getCode() { return code; }
	public String getName() { return name; }
	public String getDescription() { return description; }
	public Discipline getDiscipline() { return discipline; }
	public String getImageUrl() { return imageUrl; }
	public Integer getDefaultMinimumEnrollment() { return defaultMinimumEnrollment; }
	public CourseStatus getStatus() { return status; }
	public Instant getCreatedAt() { return createdAt; }
	public Instant getUpdatedAt() { return updatedAt; }
	public Long getVersion() { return version; }
}
