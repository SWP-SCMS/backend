package com.scms.backend.reception;

public class ReceptionMemberValidationException extends RuntimeException {

	private final String field;

	public ReceptionMemberValidationException(String field, String message) {
		super(message);
		this.field = field;
	}

	public String getField() {
		return field;
	}
}
