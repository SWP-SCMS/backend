package com.scms.backend.notification;

public class NotificationValidationException extends RuntimeException {

	private final String field;

	public NotificationValidationException(String field, String message) {
		super(message);
		this.field = field;
	}

	public String getField() {
		return field;
	}
}
