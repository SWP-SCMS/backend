package com.scms.backend.scheduling;

import org.springframework.http.HttpStatus;

public class AttendanceException extends RuntimeException {

	final HttpStatus status;

	final String code;

	AttendanceException(HttpStatus s, String c, String m) {
		super(m);
		status = s;
		code = c;
	}

	public HttpStatus getStatus() {
		return status;
	}

	public String getCode() {
		return code;
	}

	public static AttendanceException missing() {
		return new AttendanceException(HttpStatus.NOT_FOUND, "SESSION_NOT_FOUND", "Session not found.");
	}

	public static AttendanceException visit() {
		return new AttendanceException(HttpStatus.CONFLICT, "CENTER_VISIT_REQUIRED", "A current Center Visit is required.");
	}

	public static AttendanceException closed() {
		return new AttendanceException(HttpStatus.CONFLICT, "ATTENDANCE_WINDOW_CLOSED", "Attendance window is closed.");
	}

	public static AttendanceException invalid() {
		return new AttendanceException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "status is required.");
	}
}
