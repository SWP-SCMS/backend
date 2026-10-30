package com.scms.backend.course;

import org.springframework.http.HttpStatus;

public class CourseOrderException extends RuntimeException {

	private final HttpStatus status;
	private final String code;

	private CourseOrderException(HttpStatus status, String code, String message) {
		super(message);
		this.status = status;
		this.code = code;
	}

	static CourseOrderException keyRequired() {
		return new CourseOrderException(HttpStatus.BAD_REQUEST, "COURSE_IDEMPOTENCY_KEY_REQUIRED",
			"Idempotency-Key must be nonblank and at most 100 characters.");
	}

	static CourseOrderException keyReused() {
		return new CourseOrderException(HttpStatus.CONFLICT, "COURSE_IDEMPOTENCY_KEY_REUSED",
			"Idempotency-Key was already used for another course order request.");
	}

	static CourseOrderException runUnavailable() {
		return new CourseOrderException(HttpStatus.NOT_FOUND, "COURSE_RUN_NOT_AVAILABLE",
			"The requested course run is not available.");
	}

	static CourseOrderException memberNotFound() {
		return new CourseOrderException(HttpStatus.NOT_FOUND, "MEMBER_NOT_FOUND",
			"The requested member was not found.");
	}

	static CourseOrderException memberNotActive() {
		return new CourseOrderException(HttpStatus.CONFLICT, "MEMBER_NOT_ACTIVE",
			"The member account is not active.");
	}

	static CourseOrderException registrationNotOpen() {
		return new CourseOrderException(HttpStatus.CONFLICT, "COURSE_REGISTRATION_NOT_OPEN",
			"Course registration has not opened.");
	}

	static CourseOrderException registrationClosed() {
		return new CourseOrderException(HttpStatus.CONFLICT, "COURSE_REGISTRATION_CLOSED",
			"Course registration is closed.");
	}

	static CourseOrderException reservationExists() {
		return new CourseOrderException(HttpStatus.CONFLICT, "COURSE_RESERVATION_EXISTS",
			"The member already has an effective reservation for this course run.");
	}

	static CourseOrderException capacityFull() {
		return new CourseOrderException(HttpStatus.CONFLICT, "COURSE_CAPACITY_FULL",
			"The course run has no remaining reservation capacity.");
	}

	static CourseOrderException scheduleConflict() {
		return new CourseOrderException(HttpStatus.CONFLICT, "COURSE_SCHEDULE_CONFLICT",
			"The course schedule conflicts with the member's existing schedule.");
	}

	static CourseOrderException orderNotFound() {
		return new CourseOrderException(HttpStatus.NOT_FOUND, "COURSE_ORDER_NOT_FOUND",
			"The requested course order was not found.");
	}

	public HttpStatus getStatus() { return status; }
	public String getCode() { return code; }
}
