package com.scms.backend.course;

import org.springframework.http.HttpStatus;

public class CourseWaitlistException extends RuntimeException {

	private final HttpStatus status;

	private final String code;

	private CourseWaitlistException(HttpStatus status, String code, String message) {
		super(message);
		this.status = status;
		this.code = code;
	}

	public static CourseWaitlistException unavailable() {
		return new CourseWaitlistException(HttpStatus.CONFLICT, "COURSE_WAITLIST_UNAVAILABLE", "The CourseRun is not available for waitlisting.");
	}

	public static CourseWaitlistException capacityAvailable() {
		return new CourseWaitlistException(HttpStatus.CONFLICT, "COURSE_WAITLIST_CAPACITY_AVAILABLE", "The CourseRun still has capacity; create an order directly.");
	}

	public static CourseWaitlistException duplicate() {
		return new CourseWaitlistException(HttpStatus.CONFLICT, "COURSE_WAITLIST_DUPLICATE", "The Member already has an active Course waitlist entry.");
	}

	public static CourseWaitlistException notFound() {
		return new CourseWaitlistException(HttpStatus.NOT_FOUND, "COURSE_WAITLIST_NOT_FOUND", "No active Course waitlist entry was found.");
	}

	public static CourseWaitlistException invitationExpired() {
		return new CourseWaitlistException(HttpStatus.CONFLICT, "COURSE_WAITLIST_INVITATION_EXPIRED", "The Course waitlist invitation has expired.");
	}

	public HttpStatus getStatus() {
		return status;
	}

	public String getCode() {
		return code;
	}
}
