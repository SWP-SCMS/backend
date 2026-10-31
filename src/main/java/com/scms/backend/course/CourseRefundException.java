package com.scms.backend.course;

import org.springframework.http.HttpStatus;

public final class CourseRefundException extends RuntimeException {
	private final HttpStatus status;
	private final String code;

	private CourseRefundException(HttpStatus status, String code, String message) {
		super(message); this.status = status; this.code = code;
	}

	public HttpStatus status() { return status; }
	public String code() { return code; }

	static CourseRefundException invalid(String message) {
		return new CourseRefundException(HttpStatus.BAD_REQUEST, "COURSE_REFUND_INVALID", message);
	}
	static CourseRefundException notFound() {
		return new CourseRefundException(HttpStatus.NOT_FOUND, "COURSE_REFUND_NOT_FOUND",
			"The Course refund obligation was not found.");
	}
	static CourseRefundException conflict(String code, String message) {
		return new CourseRefundException(HttpStatus.CONFLICT, code, message);
	}
}
