package com.scms.backend.scheduling;

import org.springframework.http.HttpStatus;

public class RoomException extends RuntimeException {

	private final HttpStatus status;
	private final String code;
	private final String field;

	private RoomException(HttpStatus status, String code, String field, String message) {
		super(message);
		this.status = status;
		this.code = code;
		this.field = field;
	}

	static RoomException validation(String field, String message) {
		return new RoomException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", field, message);
	}

	static RoomException notFound() {
		return new RoomException(HttpStatus.NOT_FOUND, "ROOM_NOT_FOUND", null,
			"The requested room was not found.");
	}

	static RoomException duplicateName() {
		return new RoomException(HttpStatus.CONFLICT, "ROOM_NAME_CONFLICT", null,
			"A room with this name already exists.");
	}

	static RoomException capacityConflict() {
		return new RoomException(HttpStatus.CONFLICT, "ROOM_CAPACITY_CONFLICT", null,
			"Room capacity cannot be lower than an active session capacity.");
	}

	static RoomException activeSessionsConflict(java.util.List<java.util.UUID> sessionIds) {
		return new RoomException(HttpStatus.CONFLICT, "ROOM_HAS_ACTIVE_SESSIONS", "affectedSessionIds",
			"Room cannot be disabled while sessions are in progress or scheduled in the future: " + sessionIds);
	}

	static RoomException suspensionValidation(String field, String message) {
		return new RoomException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", field, message);
	}

	static RoomException suspensionConflict() {
		return new RoomException(HttpStatus.CONFLICT, "ROOM_SUSPENSION_CONFLICT", null,
			"The room suspension overlaps an existing suspension or Session.");
	}

	public HttpStatus getStatus() { return status; }
	public String getCode() { return code; }
	public String getField() { return field; }
}
