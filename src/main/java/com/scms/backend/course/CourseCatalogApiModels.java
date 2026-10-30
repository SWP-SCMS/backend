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

record CatalogCourseResponse(UUID id, String code, String name, String description, UUID disciplineId,
	String disciplineName, String imageUrl) {

	static CatalogCourseResponse from(Course course) {
		return new CatalogCourseResponse(course.getId(), course.getCode(), course.getName(), course.getDescription(),
			course.getDiscipline().getId(), course.getDiscipline().getName(), course.getImageUrl());
	}
}

record CatalogCoursePageResponse(List<CatalogCourseResponse> content, int page, int size, long totalElements,
	int totalPages) {

	static CatalogCoursePageResponse from(org.springframework.data.domain.Page<Course> page) {
		return new CatalogCoursePageResponse(page.getContent().stream().map(CatalogCourseResponse::from).toList(),
			page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
	}
}

record CatalogCourseRunResponse(UUID id, String runCode, UUID courseId, String courseCode, String courseName,
	UUID primaryCoachId, String primaryCoachName, BigInteger tuitionVnd, int capacity,
	Instant registrationOpensAt, Instant registrationClosesAt, RegistrationStatus registrationStatus,
	Instant plannedStartAt, Instant plannedEndAt, int totalUnits,
	@Schema(allowableValues = "OPEN_FOR_REGISTRATION") CourseRunStatus status) {

	static CatalogCourseRunResponse from(CourseRun run, Instant now) {
		return new CatalogCourseRunResponse(run.getId(), run.getRunCode(), run.getCourse().getId(),
			run.getCourse().getCode(), run.getCourse().getName(), run.getPrimaryCoach().getId(),
			run.getPrimaryCoach().getFullName(), run.getTuitionVnd(), run.getCapacity(),
			run.getRegistrationOpensAt(), run.getRegistrationClosesAt(),
			RegistrationStatus.at(now, run.getRegistrationOpensAt(), run.getRegistrationClosesAt()),
			run.getPlannedStartAt(), run.getPlannedEndAt(), run.getTotalUnits(), run.getStatus());
	}
}

record CatalogCourseRunPageResponse(List<CatalogCourseRunResponse> content, int page, int size,
	long totalElements, int totalPages) {

	static CatalogCourseRunPageResponse from(org.springframework.data.domain.Page<CourseRun> page, Instant now) {
		return new CatalogCourseRunPageResponse(
			page.getContent().stream().map(run -> CatalogCourseRunResponse.from(run, now)).toList(),
			page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
	}
}

record CatalogCourseSessionResponse(UUID id, int unitNumber, UUID roomId, String roomName, UUID coachId,
	String coachName, Instant startAt, Instant endAt, Instant originalStartAt, Instant originalEndAt,
	BigInteger allocationVnd, CourseSessionStatus status) {

	static CatalogCourseSessionResponse from(CourseSession session) {
		return new CatalogCourseSessionResponse(session.getId(), session.getUnitNumber(), session.getRoom().getId(),
			session.getRoom().getName(), session.getCoach().getId(), session.getCoach().getFullName(),
			session.getStartAt(), session.getEndAt(), session.getOriginalStartAt(), session.getOriginalEndAt(),
			session.getAllocationVnd(), session.getStatus());
	}
}

record CatalogCourseRunDetailResponse(UUID id, String runCode, UUID courseId, String courseCode, String courseName,
	UUID primaryCoachId, String primaryCoachName, BigInteger tuitionVnd, int capacity,
	Instant registrationOpensAt, Instant registrationClosesAt, RegistrationStatus registrationStatus,
	Instant plannedStartAt, Instant plannedEndAt, int totalUnits,
	@Schema(allowableValues = "OPEN_FOR_REGISTRATION") CourseRunStatus status,
	List<CatalogCourseSessionResponse> sessions) {

	CatalogCourseRunDetailResponse(CatalogCourseRunResponse run, List<CatalogCourseSessionResponse> sessions) {
		this(run.id(), run.runCode(), run.courseId(), run.courseCode(), run.courseName(), run.primaryCoachId(),
			run.primaryCoachName(), run.tuitionVnd(), run.capacity(), run.registrationOpensAt(),
			run.registrationClosesAt(), run.registrationStatus(), run.plannedStartAt(), run.plannedEndAt(),
			run.totalUnits(), run.status(), sessions);
	}
}
