package com.scms.backend.membership;

public class ReportingValidationException extends RuntimeException {

	private final String field;

	ReportingValidationException(String field, String message) {
		super(message);
		this.field = field;
	}

	public String getField() {
		return field;
	}
}
