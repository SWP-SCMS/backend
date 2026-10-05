package com.scms.backend.scheduling;

import org.springframework.http.HttpStatus;

public class SportClassException extends RuntimeException {

	private final HttpStatus status;
	private final String code;
	private final String field;

	private SportClassException(HttpStatus status, String code, String field, String message) {
		super(message);
		this.status = status;
		this.code = code;
		this.field = field;
	}

	static SportClassException validation(String field, String message) {
		return new SportClassException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", field, message);
	}

	static SportClassException notFound() {
		return new SportClassException(HttpStatus.NOT_FOUND, "CLASS_NOT_FOUND", null,
			"The requested class was not found.");
	}

	static SportClassException disciplineNotFound() {
		return new SportClassException(HttpStatus.NOT_FOUND, "DISCIPLINE_NOT_FOUND", null,
			"The requested discipline was not found.");
	}

	static SportClassException inactiveDiscipline() {
		return new SportClassException(HttpStatus.CONFLICT, "DISCIPLINE_INACTIVE", null,
			"An inactive discipline cannot be used to create a class.");
	}

	static SportClassException duplicateName() {
		return new SportClassException(HttpStatus.CONFLICT, "CLASS_NAME_CONFLICT", null,
			"A class with this name already exists in the discipline.");
	}

	public HttpStatus getStatus() { return status; }
	public String getCode() { return code; }
	public String getField() { return field; }
}
