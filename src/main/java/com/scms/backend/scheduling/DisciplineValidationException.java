package com.scms.backend.scheduling;

public class DisciplineValidationException extends RuntimeException {

	private final String field;

	DisciplineValidationException(String field, String message) {
		super(message);
		this.field = field;
	}

	public String getField() {
		return field;
	}
}
