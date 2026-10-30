package com.scms.backend.course;

import org.springframework.http.HttpStatus;

public class CourseAttendanceException extends RuntimeException {
	private final HttpStatus status;
	private final String code;

	private CourseAttendanceException(HttpStatus status, String code, String message) {
		super(message); this.status = status; this.code = code;
	}

	public HttpStatus getStatus() { return status; }
	public String getCode() { return code; }
	static CourseAttendanceException notFound() { return new CourseAttendanceException(HttpStatus.NOT_FOUND,
		"COURSE_SESSION_NOT_FOUND", "Course session not found."); }
	static CourseAttendanceException closed() { return new CourseAttendanceException(HttpStatus.CONFLICT,
		"COURSE_ATTENDANCE_WINDOW_CLOSED", "Course attendance window is closed."); }
	static CourseAttendanceException invalid() { return new CourseAttendanceException(HttpStatus.BAD_REQUEST,
		"VALIDATION_ERROR", "status must be PRESENT or ABSENT."); }
}
