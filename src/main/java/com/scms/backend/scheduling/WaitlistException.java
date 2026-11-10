package com.scms.backend.scheduling;

import org.springframework.http.HttpStatus;

public class WaitlistException extends RuntimeException {

	private final HttpStatus status;

	private final String code;

	private WaitlistException(HttpStatus status, String code, String message) {
		super(message);
		this.status = status;
		this.code = code;
	}

	static public WaitlistException sessionNotFull() {
		return new WaitlistException(HttpStatus.CONFLICT, "SESSION_NOT_FULL", "The Session still has capacity; book it directly.");
	}

	static public WaitlistException duplicate() {
		return new WaitlistException(HttpStatus.CONFLICT, "WAITLIST_DUPLICATE", "The Member is already waiting for this Session.");
	}

	static public WaitlistException notFound() {
		return new WaitlistException(HttpStatus.NOT_FOUND, "WAITLIST_NOT_FOUND", "No active waitlist entry was found.");
	}

	public HttpStatus getStatus() {
		return status;
	}

	public String getCode() {
		return code;
	}
}
