package com.scms.backend.auth;

public class ChangePasswordValidationException extends RuntimeException {

	private final String field;

	public ChangePasswordValidationException(String field, String message) {
		super(message);
		this.field = field;
	}

	public String getField() {
		return field;
	}
}
