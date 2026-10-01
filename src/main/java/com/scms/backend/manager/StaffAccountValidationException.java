package com.scms.backend.manager;

public class StaffAccountValidationException extends RuntimeException {

	private final String field;

	public StaffAccountValidationException(String field, String message) {
		super(message);
		this.field = field;
	}

	public String getField() {
		return field;
	}
}
