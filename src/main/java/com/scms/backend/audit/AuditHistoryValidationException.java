package com.scms.backend.audit;

public class AuditHistoryValidationException extends RuntimeException {

	private final String field;

	public AuditHistoryValidationException(String field, String message) {
		super(message);
		this.field = field;
	}

	public String getField() {
		return field;
	}
}
