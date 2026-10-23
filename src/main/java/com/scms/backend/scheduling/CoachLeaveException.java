package com.scms.backend.scheduling;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;

public class CoachLeaveException extends RuntimeException {
	private final HttpStatus status;
	private final String code;
	private final String field;
	private final List<UUID> affectedSessionIds;
	private CoachLeaveException(HttpStatus status, String code, String field, String message) {
		this(status, code, field, message, List.of());
	}
	private CoachLeaveException(HttpStatus status, String code, String field, String message,
			List<UUID> affectedSessionIds) {
		super(message); this.status = status; this.code = code; this.field = field;
		this.affectedSessionIds = List.copyOf(affectedSessionIds);
	}
	static CoachLeaveException validation(String field, String message) {
		return new CoachLeaveException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", field, message);
	}
	static CoachLeaveException notFound() { return new CoachLeaveException(HttpStatus.NOT_FOUND,
		"COACH_LEAVE_NOT_FOUND", null, "The requested Coach leave request was not found."); }
	static CoachLeaveException conflict(String code, String message) {
		return new CoachLeaveException(HttpStatus.CONFLICT, code, null, message);
	}
	static CoachLeaveException affectedSessions(List<UUID> sessionIds) {
		return new CoachLeaveException(HttpStatus.CONFLICT, "COACH_LEAVE_HAS_AFFECTED_SESSIONS", null,
			"Resolve all affected sessions before approving this leave request.", sessionIds);
	}
	public HttpStatus getStatus() { return status; }
	public String getCode() { return code; }
	public String getField() { return field; }
	public List<UUID> getAffectedSessionIds() { return affectedSessionIds; }
}

