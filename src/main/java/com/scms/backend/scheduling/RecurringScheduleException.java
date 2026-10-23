package com.scms.backend.scheduling;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;

public class RecurringScheduleException extends RuntimeException {

	private final HttpStatus status;
	private final String code;
	private final String field;
	private final List<UUID> eligibleSessionIds;
	private final List<UUID> ineligibleSessionIds;
	private final Integer roomCapacity;
	private final Long occupiedCapacity;
	private final Integer requestedCapacity;

	private RecurringScheduleException(HttpStatus status, String code, String field, String message) {
		this(status, code, field, message, List.of(), List.of(), null, null, null);
	}

	private RecurringScheduleException(HttpStatus status, String code, String field, String message,
			List<UUID> eligibleSessionIds, List<UUID> ineligibleSessionIds) {
		this(status, code, field, message, eligibleSessionIds, ineligibleSessionIds, null, null, null);
	}

	private RecurringScheduleException(HttpStatus status, String code, String field, String message,
			List<UUID> eligibleSessionIds, List<UUID> ineligibleSessionIds, Integer roomCapacity,
			Long occupiedCapacity, Integer requestedCapacity) {
		super(message);
		this.status = status;
		this.code = code;
		this.field = field;
		this.eligibleSessionIds = List.copyOf(eligibleSessionIds);
		this.ineligibleSessionIds = List.copyOf(ineligibleSessionIds);
		this.roomCapacity = roomCapacity;
		this.occupiedCapacity = occupiedCapacity;
		this.requestedCapacity = requestedCapacity;
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

	static RecurringScheduleException legacyYoga() {
		return new RecurringScheduleException(HttpStatus.BAD_REQUEST, "LEGACY_YOGA_UNSUPPORTED", "classId",
			"Legacy Yoga classes are retained for history and cannot receive new Sessions.");
	}

	static RecurringScheduleException coachInvalid() {
		return new RecurringScheduleException(HttpStatus.CONFLICT, "COACH_NOT_ACTIVE", null,
			"The selected account is not an active Coach.");
	}

	static RecurringScheduleException coachOnLeave() {
		return new RecurringScheduleException(HttpStatus.CONFLICT, "COACH_ON_APPROVED_LEAVE", "coachId",
			"The selected Coach is on approved leave during this interval.");
	}

	static RecurringScheduleException capacityConflict() {
		return new RecurringScheduleException(HttpStatus.CONFLICT, "ROOM_CAPACITY_BELOW_SESSION_CAPACITY", "capacity",
			"Session capacity cannot exceed room capacity.");
	}

	static RecurringScheduleException roomTimeConflict() {
		return new RecurringScheduleException(HttpStatus.CONFLICT, "ROOM_TIME_CONFLICT", null,
			"The exclusive Room already has an overlapping Session.");
	}

	static RecurringScheduleException sharedRoomCapacityExceeded(int roomCapacity, long occupiedCapacity,
			int requestedCapacity) {
		return new RecurringScheduleException(HttpStatus.CONFLICT, "SHARED_ROOM_CAPACITY_EXCEEDED", null,
			"The shared Room does not have enough capacity for this Session.", List.of(), List.of(),
			roomCapacity, occupiedCapacity, requestedCapacity);
	}

	static RecurringScheduleException ptCapacity() {
		return new RecurringScheduleException(HttpStatus.BAD_REQUEST, "PT_CAPACITY_MUST_BE_ONE", "capacity",
			"A PT_1_1 Session must have capacity equal to one.");
	}

	static RecurringScheduleException sessionConflict() {
		return new RecurringScheduleException(HttpStatus.CONFLICT, "SESSION_CONFLICT", null,
			"The Coach or Room already has an overlapping Session.");
	}

	static RecurringScheduleException sessionNotScheduled() {
		return new RecurringScheduleException(HttpStatus.CONFLICT, "SESSION_NOT_SCHEDULED", null,
			"Only a scheduled Session can be changed.");
	}

	static RecurringScheduleException rescheduleWindowClosed() {
		return new RecurringScheduleException(HttpStatus.CONFLICT, "SESSION_RESCHEDULE_WINDOW_CLOSED", null,
			"A Session must be rescheduled at least 24 hours before its current start time.");
	}

	static RecurringScheduleException versionConflict() {
		return new RecurringScheduleException(HttpStatus.CONFLICT, "SESSION_VERSION_CONFLICT", null,
			"The Session changed after it was loaded. Refresh and try again.");
	}

	static RecurringScheduleException memberTimeConflict() {
		return new RecurringScheduleException(HttpStatus.CONFLICT, "SESSION_MEMBER_TIME_CONFLICT", null,
			"One or more booked Members have another Booking overlapping the requested time.");
	}

	static RecurringScheduleException bulkSelectionChanged(List<UUID> eligibleSessionIds,
			List<UUID> ineligibleSessionIds) {
		return new RecurringScheduleException(HttpStatus.CONFLICT, "SESSION_BULK_SELECTION_CHANGED", null,
			"One or more selected Sessions are no longer eligible for cancellation.",
			eligibleSessionIds, ineligibleSessionIds);
	}

	static RecurringScheduleException scheduleConflict() {
		return new RecurringScheduleException(HttpStatus.CONFLICT, "RECURRING_SCHEDULE_CONFLICT", null,
			"The recurring schedule conflicts with an existing Session.");
	}

	public HttpStatus getStatus() { return status; }
	public String getCode() { return code; }
	public String getField() { return field; }
	public List<UUID> getEligibleSessionIds() { return eligibleSessionIds; }
	public List<UUID> getIneligibleSessionIds() { return ineligibleSessionIds; }
	public Integer getRoomCapacity() { return roomCapacity; }
	public Long getOccupiedCapacity() { return occupiedCapacity; }
	public Integer getRequestedCapacity() { return requestedCapacity; }
}
