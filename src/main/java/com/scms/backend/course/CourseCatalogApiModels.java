package com.scms.backend.course;

import java.math.BigInteger;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

enum RegistrationStatus {
	UPCOMING,
	OPEN,
	CLOSED;

	static RegistrationStatus at(Instant now, Instant opensAt, Instant closesAt) {
		if (now.isBefore(opensAt)) return UPCOMING;
		return now.isBefore(closesAt) ? OPEN : CLOSED;
	}
}

record CatalogCourseResponse(
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID id,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED) String code,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED, types = { "string", "null" }) String description,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID disciplineId,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED) String disciplineName,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED, types = { "string", "null" }) String imageUrl) {

	static CatalogCourseResponse from(Course course) {
		return new CatalogCourseResponse(course.getId(), course.getCode(), course.getName(), course.getDescription(),
			course.getDiscipline().getId(), course.getDiscipline().getName(), course.getImageUrl());
	}
}

record CatalogCoursePageResponse(
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<CatalogCourseResponse> content,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0") int page,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "1", maximum = "100") int size,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0") long totalElements,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0") int totalPages) {

	static CatalogCoursePageResponse from(org.springframework.data.domain.Page<Course> page) {
		return new CatalogCoursePageResponse(page.getContent().stream().map(CatalogCourseResponse::from).toList(),
			page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
	}
}

record CatalogCourseRunResponse(
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID id,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED) String runCode,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID courseId,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED) String courseCode,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED) String courseName,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID primaryCoachId,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED) String primaryCoachName,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "1", maximum = "9999999999999999999")
	BigInteger tuitionVnd,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "1") int capacity,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant registrationOpensAt,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant registrationClosesAt,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED) RegistrationStatus registrationStatus,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant plannedStartAt,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant plannedEndAt,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "1") int totalUnits,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = "OPEN_FOR_REGISTRATION")
	CourseRunStatus status) {

	static CatalogCourseRunResponse from(CourseRun run, Instant now) {
		return new CatalogCourseRunResponse(run.getId(), run.getRunCode(), run.getCourse().getId(),
			run.getCourse().getCode(), run.getCourse().getName(), run.getPrimaryCoach().getId(),
			run.getPrimaryCoach().getFullName(), run.getTuitionVnd(), run.getCapacity(),
			run.getRegistrationOpensAt(), run.getRegistrationClosesAt(),
			RegistrationStatus.at(now, run.getRegistrationOpensAt(), run.getRegistrationClosesAt()),
			run.getPlannedStartAt(), run.getPlannedEndAt(), run.getTotalUnits(), run.getStatus());
	}
}

record CatalogCourseRunPageResponse(
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<CatalogCourseRunResponse> content,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0") int page,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "1", maximum = "100") int size,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0") long totalElements,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0") int totalPages) {

	static CatalogCourseRunPageResponse from(org.springframework.data.domain.Page<CourseRun> page, Instant now) {
		return new CatalogCourseRunPageResponse(
			page.getContent().stream().map(run -> CatalogCourseRunResponse.from(run, now)).toList(),
			page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
	}
}

record CatalogCourseSessionResponse(
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID id,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "1") int unitNumber,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID roomId,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED) String roomName,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID coachId,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED) String coachName,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant startAt,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant endAt,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant originalStartAt,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant originalEndAt,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "1", maximum = "9999999999999999999")
	BigInteger allocationVnd,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED) CourseSessionStatus status) {

	static CatalogCourseSessionResponse from(CourseSession session) {
		return new CatalogCourseSessionResponse(session.getId(), session.getUnitNumber(), session.getRoom().getId(),
			session.getRoom().getName(), session.getCoach().getId(), session.getCoach().getFullName(),
			session.getStartAt(), session.getEndAt(), session.getOriginalStartAt(), session.getOriginalEndAt(),
			session.getAllocationVnd(), session.getStatus());
	}
}

record CatalogCourseRunDetailResponse(
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID id,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED) String runCode,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID courseId,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED) String courseCode,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED) String courseName,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID primaryCoachId,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED) String primaryCoachName,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "1", maximum = "9999999999999999999")
	BigInteger tuitionVnd,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "1") int capacity,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant registrationOpensAt,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant registrationClosesAt,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED) RegistrationStatus registrationStatus,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant plannedStartAt,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant plannedEndAt,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "1") int totalUnits,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = "OPEN_FOR_REGISTRATION")
	CourseRunStatus status,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<CatalogCourseSessionResponse> sessions) {

	CatalogCourseRunDetailResponse(CatalogCourseRunResponse run, List<CatalogCourseSessionResponse> sessions) {
		this(run.id(), run.runCode(), run.courseId(), run.courseCode(), run.courseName(), run.primaryCoachId(),
			run.primaryCoachName(), run.tuitionVnd(), run.capacity(), run.registrationOpensAt(),
			run.registrationClosesAt(), run.registrationStatus(), run.plannedStartAt(), run.plannedEndAt(),
			run.totalUnits(), run.status(), sessions);
	}
}
