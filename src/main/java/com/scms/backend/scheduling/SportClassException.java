package com.scms.backend.scheduling;

import java.util.List;

import org.springframework.http.HttpStatus;

public class SportClassException extends RuntimeException {

	private final HttpStatus status;
	private final String code;
	private final String field;
	private final List<DisciplineSessionConflict> affectedSessions;

	private SportClassException(HttpStatus status, String code, String field, String message) {
		this(status, code, field, message, List.of());
	}

	private SportClassException(HttpStatus status, String code, String field, String message,
			List<DisciplineSessionConflict> affectedSessions) {
		super(message);
		this.status = status;
		this.code = code;
		this.field = field;
		this.affectedSessions = List.copyOf(affectedSessions);
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

	static SportClassException unsupportedType() {
		return new SportClassException(HttpStatus.BAD_REQUEST, "CLASS_TYPE_UNSUPPORTED", "classType",
			"New classes must use GROUP or PT_1_1.");
	}

	static SportClassException duplicateName() {
		return new SportClassException(HttpStatus.CONFLICT, "CLASS_NAME_CONFLICT", null,
			"A class with this name already exists in the discipline.");
	}

	static SportClassException activeSessions(List<DisciplineSessionConflict> affectedSessions) {
		return new SportClassException(HttpStatus.CONFLICT, "CLASS_HAS_ACTIVE_SESSIONS", null,
			"A class cannot be inactive while sessions are in progress or have not ended.", affectedSessions);
	}

	public HttpStatus getStatus() { return status; }
	public String getCode() { return code; }
	public String getField() { return field; }
	public List<DisciplineSessionConflict> getAffectedSessions() { return affectedSessions; }
}
