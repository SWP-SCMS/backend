package com.scms.backend.membership;

public class MembershipPlanValidationException extends RuntimeException {
	private final String field;

	public MembershipPlanValidationException(String field, String message) {
		super(message);
		this.field = field;
	}

	public MembershipPlanValidationException(String message) { this("request", message); }

	public String getField() { return field; }
}
