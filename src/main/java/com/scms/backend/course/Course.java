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

	@Column(name = "submitted_by_account_id", updatable = false)
	private UUID submittedByAccountId;

	@Column(name = "submitted_at", updatable = false)
	private Instant submittedAt;

	@Column(name = "reviewed_by_account_id")
	private UUID reviewedByAccountId;

	@Column(name = "reviewed_at")
	private Instant reviewedAt;

	@Column(name = "review_reason", length = 1000)
	private String reviewReason;

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
		this(id, code, name, description, discipline, imageUrl, defaultMinimumEnrollment, CourseStatus.DRAFT, null);
	}

	private Course(UUID id, String code, String name, String description, Discipline discipline, String imageUrl,
			Integer defaultMinimumEnrollment, CourseStatus status, UUID submittedByAccountId) {
		this.id = id;
		this.code = code;
		this.name = name;
		this.description = description;
		this.discipline = discipline;
		this.imageUrl = imageUrl;
		this.defaultMinimumEnrollment = defaultMinimumEnrollment;
		this.status = status;
		this.submittedByAccountId = submittedByAccountId;
		this.submittedAt = submittedByAccountId == null ? null : Instant.now();
	}

	static Course submittedForApproval(UUID id, String code, String name, String description, Discipline discipline,
			String imageUrl, Integer defaultMinimumEnrollment, UUID receptionistId) {
		return new Course(id, code, name, description, discipline, imageUrl, defaultMinimumEnrollment,
			CourseStatus.PENDING_APPROVAL, receptionistId);
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

	void approve(UUID managerId, String reason) {
		if (status != CourseStatus.PENDING_APPROVAL) throw CourseException.invalidApprovalTransition();
		status = CourseStatus.DRAFT;
		reviewedByAccountId = managerId;
		reviewedAt = Instant.now();
		reviewReason = reason;
		updatedAt = reviewedAt;
	}

	void reject(UUID managerId, String reason) {
		if (status != CourseStatus.PENDING_APPROVAL) throw CourseException.invalidApprovalTransition();
		status = CourseStatus.REJECTED;
		reviewedByAccountId = managerId;
		reviewedAt = Instant.now();
		reviewReason = reason;
		updatedAt = reviewedAt;
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
	public UUID getSubmittedByAccountId() { return submittedByAccountId; }
	public Instant getSubmittedAt() { return submittedAt; }
	public UUID getReviewedByAccountId() { return reviewedByAccountId; }
	public Instant getReviewedAt() { return reviewedAt; }
	public String getReviewReason() { return reviewReason; }
	public CourseStatus getStatus() { return status; }
	public Instant getCreatedAt() { return createdAt; }
	public Instant getUpdatedAt() { return updatedAt; }
	public Long getVersion() { return version; }
}
