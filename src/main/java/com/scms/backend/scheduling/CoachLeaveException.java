package com.scms.backend.scheduling;

import org.springframework.http.HttpStatus;

public class CoachLeaveException extends RuntimeException {
	private final HttpStatus status;
	private final String code;
	private final String field;
	private CoachLeaveException(HttpStatus status, String code, String field, String message) {
		super(message); this.status = status; this.code = code; this.field = field;
	}
	static CoachLeaveException validation(String field, String message) {
		return new CoachLeaveException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", field, message);
	}
	static CoachLeaveException notFound() { return new CoachLeaveException(HttpStatus.NOT_FOUND,
		"COACH_LEAVE_NOT_FOUND", null, "The requested Coach leave request was not found."); }
	static CoachLeaveException conflict(String code, String message) {
		return new CoachLeaveException(HttpStatus.CONFLICT, code, null, message);
	}
	public HttpStatus getStatus() { return status; }
	public String getCode() { return code; }
	public String getField() { return field; }
}

