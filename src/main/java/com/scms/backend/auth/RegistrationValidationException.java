package com.scms.backend.auth;

public class RegistrationValidationException extends RuntimeException {

	private final String field;

	RegistrationValidationException(String field, String message) {
		super(message);
		this.field = field;
	}

	public String getField() {
		return field;
	}
}
