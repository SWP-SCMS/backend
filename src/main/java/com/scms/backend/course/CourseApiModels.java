package com.scms.backend.course;

import java.math.BigInteger;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

record CourseCreateRequest(
	@NotBlank @Size(max = 50) String code,
	@NotBlank @Size(max = 200) String name,
	@Size(max = CourseFieldLimits.DESCRIPTION) @Schema(types = { "string", "null" }) String description,
	@NotNull UUID disciplineId,
	@Size(max = CourseFieldLimits.IMAGE_URL) @Schema(types = { "string", "null" }) String imageUrl,
	@Positive Integer defaultMinimumEnrollment) { }

final class CoursePatchRequest {
	@NotNull
	@PositiveOrZero
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED)
	private Long expectedVersion;
	private boolean namePresent;
	@Pattern(regexp = ".*\\S.*", message = "must not be blank")
	@Size(max = 200)
	private String name;
	private boolean descriptionPresent;
	@Size(max = CourseFieldLimits.DESCRIPTION)
	@Schema(types = { "string", "null" })
	private String description;
	private boolean imageUrlPresent;
	@Size(max = CourseFieldLimits.IMAGE_URL)
	@Schema(types = { "string", "null" })
	private String imageUrl;
	private boolean defaultMinimumEnrollmentPresent;
	@Positive
	@Schema(types = { "integer", "null" })
	private Integer defaultMinimumEnrollment;

	@JsonSetter("expectedVersion")
	public void setExpectedVersion(Long expectedVersion) {
		this.expectedVersion = expectedVersion;
	}

	@JsonSetter("name")
	public void setName(String name) {
		this.namePresent = true;
		this.name = name;
	}

	@JsonSetter("description")
	public void setDescription(String description) {
		this.descriptionPresent = true;
		this.description = description;
	}

	@JsonSetter("imageUrl")
	public void setImageUrl(String imageUrl) {
		this.imageUrlPresent = true;
		this.imageUrl = imageUrl;
	}

	@JsonSetter("defaultMinimumEnrollment")
	public void setDefaultMinimumEnrollment(Integer defaultMinimumEnrollment) {
		this.defaultMinimumEnrollmentPresent = true;
		this.defaultMinimumEnrollment = defaultMinimumEnrollment;
	}

	@JsonAnySetter
	public void rejectUnknownField(String fieldName, Object ignoredValue) {
		throw new IllegalArgumentException("Unknown course patch field: " + fieldName);
	}

	Long expectedVersion() { return expectedVersion; }
	boolean hasName() { return namePresent; }
	String name() { return name; }
	boolean hasDescription() { return descriptionPresent; }
	String description() { return description; }
	boolean hasImageUrl() { return imageUrlPresent; }
	String imageUrl() { return imageUrl; }
	boolean hasDefaultMinimumEnrollment() { return defaultMinimumEnrollmentPresent; }
	Integer defaultMinimumEnrollment() { return defaultMinimumEnrollment; }

	@Schema(maxLength = CourseFieldLimits.DESCRIPTION, types = { "string", "null" })
	public String getDescription() { return description; }

	@Schema(maxLength = CourseFieldLimits.IMAGE_URL, types = { "string", "null" })
	public String getImageUrl() { return imageUrl; }

	boolean hasMutableField() {
		return namePresent || descriptionPresent || imageUrlPresent || defaultMinimumEnrollmentPresent;
	}
}

final class CourseFieldLimits {
	public static final int DESCRIPTION = 2000;
	public static final int IMAGE_URL = 500;

	private CourseFieldLimits() { }
}

record VersionRequest(@NotNull @PositiveOrZero Long expectedVersion) { }

record CourseRunCreateRequest(
	@NotBlank @Size(max = 50) String runCode,
	@NotNull UUID courseId,
	@NotNull UUID primaryCoachId,
	@NotNull @Positive @Digits(integer = 19, fraction = 0) BigInteger tuitionVnd,
	@Positive int capacity,
	@Positive int minimumEnrollment,
	@NotNull Instant registrationOpensAt,
	@NotNull Instant registrationClosesAt,
	@NotNull Instant plannedStartAt,
	@NotNull Instant plannedEndAt,
	@Positive int totalUnits,
	boolean namedCoachCommitment) { }

record CourseRunPatchRequest(
	@NotNull @PositiveOrZero Long expectedVersion,
	@NotNull @Positive Integer minimumEnrollment) { }

record CourseRunOpenRequest(
	@NotNull @PositiveOrZero Long expectedVersion,
	@NotBlank @Size(max = 100) String policyVersion) { }

record CourseSessionCreateRequest(
	@Positive int unitNumber,
	@NotNull UUID roomId,
	@NotNull UUID coachId,
	@NotNull Instant startAt,
	@NotNull Instant endAt,
	@NotNull @Positive @Digits(integer = 19, fraction = 0) BigInteger allocationVnd) { }

record CourseSessionPatchRequest(
	@NotNull @PositiveOrZero Long expectedVersion,
	UUID roomId,
	UUID coachId,
	@NotNull Instant startAt,
	@NotNull Instant endAt) { }

record CourseResponse(UUID id, String code, String name, String description, UUID disciplineId,
	String disciplineName, String imageUrl, Integer defaultMinimumEnrollment, CourseStatus status,
	Instant createdAt, Instant updatedAt, Long version) {
	static CourseResponse from(Course course) {
		return new CourseResponse(course.getId(), course.getCode(), course.getName(), course.getDescription(),
			course.getDiscipline().getId(), course.getDiscipline().getName(), course.getImageUrl(),
			course.getDefaultMinimumEnrollment(), course.getStatus(), course.getCreatedAt(), course.getUpdatedAt(),
			course.getVersion());
	}
}

record CoursePageResponse(List<CourseResponse> content, int page, int size, long totalElements, int totalPages) {
	static CoursePageResponse from(org.springframework.data.domain.Page<Course> page) {
		return new CoursePageResponse(page.getContent().stream().map(CourseResponse::from).toList(),
			page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
	}
}

record CourseRunResponse(UUID id, String runCode, UUID courseId, String courseCode, String courseName,
	UUID primaryCoachId, String primaryCoachName, BigInteger tuitionVnd, int capacity, int minimumEnrollment,
	Instant registrationOpensAt, Instant registrationClosesAt, Instant plannedStartAt, Instant plannedEndAt,
	int totalUnits, boolean namedCoachCommitment, String policyVersionSnapshot, CourseRunStatus status,
	Instant createdAt, Instant updatedAt, Long version) {
	static CourseRunResponse from(CourseRun run) {
		return new CourseRunResponse(run.getId(), run.getRunCode(), run.getCourse().getId(),
			run.getCourse().getCode(), run.getCourse().getName(), run.getPrimaryCoach().getId(),
			run.getPrimaryCoach().getFullName(), run.getTuitionVnd(), run.getCapacity(),
			run.getMinimumEnrollment(), run.getRegistrationOpensAt(), run.getRegistrationClosesAt(),
			run.getPlannedStartAt(), run.getPlannedEndAt(), run.getTotalUnits(), run.isNamedCoachCommitment(),
			run.getPolicyVersionSnapshot(), run.getStatus(), run.getCreatedAt(), run.getUpdatedAt(), run.getVersion());
	}
}

record CourseRunPageResponse(List<CourseRunResponse> content, int page, int size, long totalElements,
	int totalPages) {
	static CourseRunPageResponse from(org.springframework.data.domain.Page<CourseRun> page) {
		return new CourseRunPageResponse(page.getContent().stream().map(CourseRunResponse::from).toList(),
			page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
	}
}

record CourseSessionResponse(UUID id, UUID runId, String runCode, int unitNumber, UUID roomId, String roomName,
	UUID coachId, String coachName, Instant startAt, Instant endAt, Instant originalStartAt, Instant originalEndAt,
	BigInteger allocationVnd, CourseSessionStatus status, UUID replacementSessionId, Instant createdAt,
	Instant updatedAt, Long version) {
	static CourseSessionResponse from(CourseSession session) {
		return new CourseSessionResponse(session.getId(), session.getCourseRun().getId(),
			session.getCourseRun().getRunCode(), session.getUnitNumber(), session.getRoom().getId(),
			session.getRoom().getName(), session.getCoach().getId(), session.getCoach().getFullName(),
			session.getStartAt(), session.getEndAt(), session.getOriginalStartAt(), session.getOriginalEndAt(),
			session.getAllocationVnd(), session.getStatus(),
			session.getReplacementSession() == null ? null : session.getReplacementSession().getId(),
			session.getCreatedAt(), session.getUpdatedAt(),
			session.getVersion());
	}
}

record CourseSessionDisruptionRequest(@jakarta.validation.constraints.NotNull
	@jakarta.validation.constraints.PositiveOrZero Long expectedVersion) { }

record CourseMakeupRequest(@jakarta.validation.constraints.NotNull @jakarta.validation.constraints.PositiveOrZero
	Long expectedVersion, @jakarta.validation.constraints.NotNull UUID roomId,
	@jakarta.validation.constraints.NotNull UUID coachId, @jakarta.validation.constraints.NotNull Instant startAt,
	@jakarta.validation.constraints.NotNull Instant endAt) { }
