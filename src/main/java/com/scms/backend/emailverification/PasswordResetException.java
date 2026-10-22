package com.scms.backend.emailverification;

import org.springframework.http.HttpStatus;

public class PasswordResetException extends RuntimeException {
	private final HttpStatus status;
	private final String code;
	private final String field;
	private final String fieldMessage;

	PasswordResetException(HttpStatus status, String code, String message) {
		this(status, code, message, null, null);
	}

	PasswordResetException(HttpStatus status, String code, String message, String field, String fieldMessage) {
		super(message);
		this.status = status;
		this.code = code;
		this.field = field;
		this.fieldMessage = fieldMessage;
	}

	public HttpStatus getStatus() { return status; }
	public String getCode() { return code; }
	public String getField() { return field; }
	public String getFieldMessage() { return fieldMessage; }
}
