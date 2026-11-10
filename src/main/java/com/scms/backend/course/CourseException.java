package com.scms.backend.course;

import org.springframework.http.HttpStatus;

public class CourseException extends RuntimeException {

	private final HttpStatus status;

	private final String code;

	private final String field;

	private CourseException(HttpStatus status, String code, String message) {
		this(status, code, message, null);
	}

	private CourseException(HttpStatus status, String code, String message, String field) {
		super(message);
		this.status = status;
		this.code = code;
		this.field = field;
	}

	public static CourseException sessionHasMembers() {
		return new CourseException(HttpStatus.CONFLICT, "SESSION_HAS_MEMBERS", "A Session with registered Members cannot be cancelled.");
	}

	public static CourseException validation(String field, String message) {
		return new CourseException(HttpStatus.BAD_REQUEST, "COURSE_VALIDATION_ERROR", message, field);
	}

	public static CourseException notFound() {
		return new CourseException(HttpStatus.NOT_FOUND, "COURSE_NOT_FOUND", "The requested course was not found.");
	}

	public static CourseException duplicateCode() {
		return new CourseException(HttpStatus.CONFLICT, "COURSE_CODE_CONFLICT", "A course with this code already exists.");
	}

	static CourseException invalidCourseTransition() {
		return new CourseException(HttpStatus.CONFLICT, "COURSE_INVALID_TRANSITION", "The course cannot transition from its current state.");
	}

	static CourseException invalidApprovalTransition() {
		return new CourseException(HttpStatus.CONFLICT, "COURSE_APPROVAL_INVALID_TRANSITION", "Only a pending Course submission can be approved or rejected.");
	}

	public static CourseException changed() {
		return new CourseException(HttpStatus.CONFLICT, "COURSE_CHANGED", "The course was changed by another request.");
	}

	public static CourseException runNotFound() {
		return new CourseException(HttpStatus.NOT_FOUND, "COURSE_RUN_NOT_FOUND", "The requested course run was not found.");
	}

	public static CourseException duplicateRunCode() {
		return new CourseException(HttpStatus.CONFLICT, "COURSE_RUN_CODE_CONFLICT", "A course run with this code already exists.");
	}

	public static CourseException runChanged() {
		return new CourseException(HttpStatus.CONFLICT, "COURSE_RUN_CHANGED", "The course run was changed by another request.");
	}

	public static CourseException rescheduleIdempotencyConflict() {
		return new CourseException(HttpStatus.CONFLICT, "COURSE_RESCHEDULE_IDEMPOTENCY_CONFLICT", "This Idempotency-Key was already used with a different reschedule payload.");
	}

	public static CourseException sessionNotFound() {
		return new CourseException(HttpStatus.NOT_FOUND, "COURSE_SESSION_NOT_FOUND", "The requested course session was not found.");
	}

	public static CourseException sessionChanged() {
		return new CourseException(HttpStatus.CONFLICT, "COURSE_SESSION_CHANGED", "The course session was changed by another request.");
	}

	public static CourseException invalidTransition() {
		return new CourseException(HttpStatus.CONFLICT, "COURSE_RUN_INVALID_TRANSITION", "The course run cannot transition from its current state.");
	}

	static CourseException invalidOpening() {
		return new CourseException(HttpStatus.BAD_REQUEST, "COURSE_RUN_INVALID_OPENING", "The course run cannot open with invalid pricing, enrollment, schedule, or policy details.");
	}

	static CourseException immutableRun() {
		return new CourseException(HttpStatus.CONFLICT, "COURSE_RUN_IMMUTABLE", "The course run can only be changed while it is in draft.");
	}

	public static CourseException invalidSession() {
		return new CourseException(HttpStatus.CONFLICT, "COURSE_SESSION_INVALID", "The course session cannot be changed in its current state or with the requested schedule.");
	}

	public static CourseException resourceConflict() {
		return new CourseException(HttpStatus.CONFLICT, "COURSE_SESSION_RESOURCE_CONFLICT", "The requested room or coach is unavailable for this course session.");
	}

	public static CourseException roomCapacityConflict() {
		return new CourseException(HttpStatus.CONFLICT, "COURSE_SESSION_ROOM_CAPACITY_CONFLICT", "The course run capacity cannot exceed the assigned room capacity.");
	}

	public static CourseException responseDeadline() {
		return new CourseException(HttpStatus.CONFLICT, "COURSE_SCHEDULE_CHANGE_DEADLINE_UNAVAILABLE", "The schedule change leaves no time for Members to respond.");
	}

	public static CourseException invalidResponse() {
		return new CourseException(HttpStatus.BAD_REQUEST, "COURSE_SCHEDULE_CHANGE_RESPONSE_INVALID", "The requested schedule-change response is invalid.");
	}

	public static CourseException responseNotFound() {
		return new CourseException(HttpStatus.NOT_FOUND, "COURSE_SCHEDULE_CHANGE_NOT_FOUND", "The requested schedule-change response was not found.");
	}

	public static CourseException responseConflict() {
		return new CourseException(HttpStatus.CONFLICT, "COURSE_SCHEDULE_CHANGE_RESPONSE_CONFLICT", "A different response was already recorded.");
	}

	public static CourseException responseClosed() {
		return new CourseException(HttpStatus.CONFLICT, "COURSE_SCHEDULE_CHANGE_RESPONSE_CLOSED", "The schedule-change response is no longer pending or its deadline has passed.");
	}

	public static CourseException sessionDisruptionConflict() {
		return new CourseException(HttpStatus.CONFLICT, "COURSE_SESSION_DISRUPTION_CONFLICT", "The course session cannot be cancelled or replaced in its current state.");
	}

	public HttpStatus getStatus() {
		return status;
	}

	public String getCode() {
		return code;
	}

	public String getField() {
		return field;
	}
}
