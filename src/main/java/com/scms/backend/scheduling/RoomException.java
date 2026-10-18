package com.scms.backend.scheduling;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;

public class RoomException extends RuntimeException {

	private final HttpStatus status;
	private final String code;
	private final String field;
	private final List<UUID> affectedSessionIds;
	private final UUID sessionId;
	private final UUID targetRoomId;
	private final Instant startTime;
	private final Instant endTime;
	private final UUID conflictingSessionId;

	private RoomException(HttpStatus status, String code, String field, String message) {
		this(status, code, field, message, List.of(), null, null, null, null, null);
	}

	private RoomException(HttpStatus status, String code, String field, String message,
			List<UUID> affectedSessionIds) {
		this(status, code, field, message, affectedSessionIds, null, null, null, null, null);
	}

	private RoomException(HttpStatus status, String code, String field, String message,
			List<UUID> affectedSessionIds, UUID sessionId, UUID targetRoomId, Instant startTime,
			Instant endTime, UUID conflictingSessionId) {
		super(message);
		this.status = status;
		this.code = code;
		this.field = field;
		this.affectedSessionIds = List.copyOf(affectedSessionIds);
		this.sessionId = sessionId;
		this.targetRoomId = targetRoomId;
		this.startTime = startTime;
		this.endTime = endTime;
		this.conflictingSessionId = conflictingSessionId;
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
			"Room cannot be disabled while sessions are in progress or scheduled in the future: " + sessionIds, sessionIds);
	}

	static RoomException inProgressSessionsConflict(List<UUID> sessionIds) {
		return new RoomException(HttpStatus.CONFLICT, "ROOM_HAS_IN_PROGRESS_SESSION", "affectedSessionIds",
			"Room cannot be disabled while a Session is in progress: " + sessionIds, sessionIds);
	}

	static RoomException suspensionValidation(String field, String message) {
		return new RoomException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", field, message);
	}

	static RoomException suspensionConflict() {
		return new RoomException(HttpStatus.CONFLICT, "ROOM_SUSPENSION_CONFLICT", null,
			"The room suspension overlaps an existing suspension or Session.");
	}

	static RoomException inactiveRoom() {
		return new RoomException(HttpStatus.CONFLICT, "ROOM_INACTIVE", null,
			"The target room is not active.");
	}

	static RoomException moveCapacityConflict(UUID sessionId, UUID targetRoomId, Instant startTime, Instant endTime) {
		return moveConflict("ROOM_CAPACITY_CONFLICT", "The target room cannot hold the Session.", sessionId,
			targetRoomId, startTime, endTime, null);
	}

	static RoomException moveSuspensionConflict(UUID sessionId, UUID targetRoomId, Instant startTime, Instant endTime) {
		return moveConflict("ROOM_SUSPENSION_CONFLICT", "The target room is suspended during the Session.", sessionId,
			targetRoomId, startTime, endTime, null);
	}

	static RoomException moveSessionConflict(UUID sessionId, UUID targetRoomId, Instant startTime, Instant endTime,
			UUID conflictingSessionId) {
		return moveConflict("ROOM_SESSION_CONFLICT", "The target room or Coach has an overlapping Session.", sessionId,
			targetRoomId, startTime, endTime, conflictingSessionId);
	}

	private static RoomException moveConflict(String code, String message, UUID sessionId, UUID targetRoomId,
			Instant startTime, Instant endTime, UUID conflictingSessionId) {
		return new RoomException(HttpStatus.CONFLICT, code, null, message, List.of(), sessionId, targetRoomId,
			startTime, endTime, conflictingSessionId);
	}

	static RoomException cancelNotAllowed(UUID sessionId) {
		return sessionNotScheduled(sessionId);
	}

	static RoomException sessionNotScheduled(UUID sessionId) {
		return new RoomException(HttpStatus.CONFLICT, "SESSION_NOT_SCHEDULED", null,
			"The Session is not eligible for this resolution.", List.of(sessionId));
	}

	public HttpStatus getStatus() { return status; }
	public String getCode() { return code; }
	public String getField() { return field; }
	public List<UUID> getAffectedSessionIds() { return affectedSessionIds; }
	public UUID getSessionId() { return sessionId; }
	public UUID getTargetRoomId() { return targetRoomId; }
	public Instant getStartTime() { return startTime; }
	public Instant getEndTime() { return endTime; }
	public UUID getConflictingSessionId() { return conflictingSessionId; }
}
