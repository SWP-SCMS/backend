package com.scms.backend.course;

import org.springframework.http.HttpStatus;

public class CourseException extends RuntimeException {

	private final HttpStatus status;
	private final String code;

	private CourseException(HttpStatus status, String code, String message) {
		super(message);
		this.status = status;
		this.code = code;
	}

	static CourseException invalidTransition() {
		return new CourseException(HttpStatus.CONFLICT, "COURSE_RUN_INVALID_TRANSITION",
			"The course run cannot transition from its current state.");
	}

	static CourseException invalidOpening() {
		return new CourseException(HttpStatus.BAD_REQUEST, "COURSE_RUN_INVALID_OPENING",
			"The course run cannot open with invalid pricing, enrollment, schedule, or policy details.");
	}

	static CourseException immutableRun() {
		return new CourseException(HttpStatus.CONFLICT, "COURSE_RUN_IMMUTABLE",
			"The course run can only be changed while it is in draft.");
	}

	public HttpStatus getStatus() {
		return status;
	}

	public String getCode() {
		return code;
	}
}
