package com.scms.backend.member;

public class MemberProfileValidationException extends RuntimeException {

	private final String field;

	MemberProfileValidationException(String field, String message) {
		super(message);
		this.field = field;
	}

	public String getField() {
		return field;
	}
}
