package com.scms.backend.course;

import org.springframework.http.HttpStatus;

public final class CourseTransferException extends RuntimeException {
	private final HttpStatus status;
	private final String code;

	private CourseTransferException(HttpStatus status, String code, String message) {
		super(message); this.status = status; this.code = code;
	}
	public HttpStatus status() { return status; }
	public String code() { return code; }

	static CourseTransferException invalid(String message) { return value(HttpStatus.BAD_REQUEST, "COURSE_TRANSFER_INVALID", message); }
	static CourseTransferException notFound() { return value(HttpStatus.NOT_FOUND, "COURSE_TRANSFER_NOT_FOUND", "The transfer resource was not found."); }
	static CourseTransferException conflict(String code, String message) { return value(HttpStatus.CONFLICT, code, message); }
	static CourseTransferException keyRequired() { return invalid("Idempotency-Key is required and must be at most 100 characters."); }
	private static CourseTransferException value(HttpStatus status, String code, String message) {
		return new CourseTransferException(status, code, message);
	}
}
