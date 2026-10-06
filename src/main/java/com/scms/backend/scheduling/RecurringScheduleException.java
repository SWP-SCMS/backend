package com.scms.backend.scheduling;

import org.springframework.http.HttpStatus;

public class RecurringScheduleException extends RuntimeException {

	private final HttpStatus status;
	private final String code;
	private final String field;

	private RecurringScheduleException(HttpStatus status, String code, String field, String message) {
		super(message);
		this.status = status;
		this.code = code;
		this.field = field;
	}

	static RecurringScheduleException validation(String field, String message) {
		return new RecurringScheduleException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", field, message);
	}

	static RecurringScheduleException notFound(String resource) {
		String code = resource.toUpperCase() + "_NOT_FOUND";
		return new RecurringScheduleException(HttpStatus.NOT_FOUND, code, null,
			"The requested " + resource + " was not found.");
	}

	static RecurringScheduleException inactive(String resource) {
		return new RecurringScheduleException(HttpStatus.CONFLICT, resource.toUpperCase() + "_INACTIVE", null,
			"An inactive " + resource + " cannot be scheduled.");
	}

	static RecurringScheduleException coachInvalid() {
		return new RecurringScheduleException(HttpStatus.CONFLICT, "COACH_NOT_ACTIVE", null,
			"The selected account is not an active Coach.");
	}

	static RecurringScheduleException capacityConflict() {
		return new RecurringScheduleException(HttpStatus.CONFLICT, "SESSION_CAPACITY_EXCEEDS_ROOM", "capacity",
			"Session capacity cannot exceed room capacity.");
	}

	static RecurringScheduleException sessionConflict() {
		return new RecurringScheduleException(HttpStatus.CONFLICT, "SESSION_CONFLICT", null,
			"The Coach or Room already has an overlapping Session.");
	}

	static RecurringScheduleException sessionNotScheduled() {
		return new RecurringScheduleException(HttpStatus.CONFLICT, "SESSION_NOT_SCHEDULED", null,
			"Only a scheduled Session can be changed.");
	}

	static RecurringScheduleException scheduleConflict() {
		return new RecurringScheduleException(HttpStatus.CONFLICT, "RECURRING_SCHEDULE_CONFLICT", null,
			"The recurring schedule conflicts with an existing Session.");
	}

	public HttpStatus getStatus() { return status; }
	public String getCode() { return code; }
	public String getField() { return field; }
}
