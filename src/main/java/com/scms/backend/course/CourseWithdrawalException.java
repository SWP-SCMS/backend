package com.scms.backend.course;

import org.springframework.http.HttpStatus;

public final class CourseWithdrawalException extends RuntimeException {
	private final HttpStatus status;
	private final String code;

	private CourseWithdrawalException(HttpStatus status, String code, String message) {
		super(message); this.status = status; this.code = code;
	}

	public HttpStatus status() { return status; }
	public String code() { return code; }

	static CourseWithdrawalException invalid(String message) {
		return new CourseWithdrawalException(HttpStatus.BAD_REQUEST, "COURSE_WITHDRAWAL_INVALID", message);
	}
	static CourseWithdrawalException notFound() {
		return new CourseWithdrawalException(HttpStatus.NOT_FOUND, "COURSE_WITHDRAWAL_NOT_FOUND",
			"The withdrawal request was not found.");
	}
	static CourseWithdrawalException conflict(String code, String message) {
		return new CourseWithdrawalException(HttpStatus.CONFLICT, code, message);
	}
}
