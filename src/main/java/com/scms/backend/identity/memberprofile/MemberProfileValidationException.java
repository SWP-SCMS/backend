package com.scms.backend.identity.memberprofile;

public class MemberProfileValidationException extends RuntimeException {

	private final String field;

	public MemberProfileValidationException(String field, String message) {
		super(message);
		this.field = field;
	}

	public String getField() {
		return field;
	}
}
