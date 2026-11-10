package com.scms.backend.scheduling.room;

import com.scms.backend.scheduling.ClassSession;
import com.scms.backend.scheduling.ClassSessionStatus;
import com.scms.backend.scheduling.Room;
import com.scms.backend.scheduling.RoomException;
import com.scms.backend.scheduling.RoomStatus;
import com.scms.backend.scheduling.room.RoomSuspensionService.Impact;
import com.scms.backend.scheduling.room.RoomSuspensionService.ResolutionInputs;
import com.scms.backend.scheduling.session.ClassSessionRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

final class RoomImpactPolicy {

	private final RoomRepository rooms;

	private final Clock clock;

	private final ClassSessionRepository sessions;

	RoomImpactPolicy(RoomRepository rooms, Clock clock, ClassSessionRepository sessions) {
		this.rooms = rooms;
		this.clock = clock;
		this.sessions = sessions;
	}

	Impact validateImpact(RoomImpactPreviewRequest request) {
		if (request == null)
			throw RoomException.validation("request", "must be a JSON object");
		Instant now = clock.instant();
		if (!request.permanentDisable()) {
			if (request.startTime() == null || !request.startTime().isAfter(now)) {
				throw RoomException.validation("startTime", "must be in the future");
			}
			if (request.endTime() == null || !request.endTime().isAfter(request.startTime())) {
				throw RoomException.validation("endTime", "must be later than startTime");
			}
		}
		String reason = normalizeReason(request.reason(), false);
		Instant queryStart = request.startTime() == null ? now : request.startTime();
		Instant queryEnd = request.endTime() == null ? now : request.endTime();
		return new Impact(queryStart, queryEnd, request.permanentDisable(), reason);
	}

	ResolutionInputs resolutionInputs(List<RoomSessionResolutionRequest> legacy, RoomBulkResolutionRequest bulk, List<RoomSessionResolutionRequest> overrides) {
		List<RoomSessionResolutionRequest> legacyRequests = legacy == null ? List.of() : legacy;
		List<RoomSessionResolutionRequest> overrideRequests = overrides == null ? List.of() : overrides;
		if (bulk != null && !legacyRequests.isEmpty()) {
			throw RoomException.validation("resolutions", "cannot be combined with bulkResolution");
		}
		if (bulk == null && !overrideRequests.isEmpty()) {
			throw RoomException.validation("resolutionOverrides", "requires bulkResolution");
		}
		if (bulk == null) {
			return new ResolutionInputs(null, resolutionMap(legacyRequests, "resolutions"));
		}
		return new ResolutionInputs(normalizeBulkResolution(bulk), resolutionMap(overrideRequests, "resolutionOverrides"));
	}

	RoomBulkResolutionRequest normalizeBulkResolution(RoomBulkResolutionRequest request) {
		if (request.action() == null) {
			throw RoomException.validation("bulkResolution.action", "is required");
		}
		String action = request.action().trim().toUpperCase(java.util.Locale.ROOT);
		if (!Set.of("MOVE", "CANCEL").contains(action)) {
			throw RoomException.validation("bulkResolution.action", "must be MOVE or CANCEL");
		}
		if (action.equals("MOVE") && request.targetRoomId() == null) {
			throw RoomException.validation("bulkResolution.targetRoomId", "is required for MOVE");
		}
		if (action.equals("CANCEL"))
			normalizeReason(request.reason(), true);
		return new RoomBulkResolutionRequest(action, request.targetRoomId(), request.reason());
	}

	Map<UUID, RoomSessionResolutionRequest> resolutionMap(List<RoomSessionResolutionRequest> requests, String field) {
		Map<UUID, RoomSessionResolutionRequest> result = new HashMap<>();
		for (RoomSessionResolutionRequest request : requests) {
			if (request == null || request.sessionId() == null || request.action() == null) {
				throw RoomException.validation(field, "sessionId and action are required");
			}
			String action = request.action().trim().toUpperCase(java.util.Locale.ROOT);
			if (action.isEmpty())
				throw RoomException.validation(field, "sessionId and action are required");
			RoomSessionResolutionRequest normalized = new RoomSessionResolutionRequest(request.sessionId(), action, request.targetRoomId(), request.startTime(), request.endTime(), request.reason());
			if (result.put(normalized.sessionId(), normalized) != null) {
				throw RoomException.validation(field, "session IDs must be unique");
			}
			if (!Set.of("MOVE", "RESCHEDULE", "CANCEL").contains(action)) {
				throw RoomException.validation(field + ".action", "must be MOVE, RESCHEDULE, or CANCEL");
			}
			if (action.equals("MOVE") && request.targetRoomId() == null) {
				throw RoomException.validation(field + ".targetRoomId", "is required for MOVE");
			}
			if (action.equals("RESCHEDULE") && (request.startTime() == null || request.endTime() == null)) {
				throw RoomException.validation(field + ".startTime", "startTime and endTime are required for RESCHEDULE");
			}
			if ((action.equals("CANCEL") || action.equals("RESCHEDULE")))
				normalizeReason(request.reason(), true);
		}
		return result;
	}

	Map<UUID, RoomSessionResolutionRequest> effectiveResolutions(List<ClassSession> affected, ResolutionInputs inputs) {
		if (inputs.bulk() == null)
			return inputs.explicit();
		Map<UUID, RoomSessionResolutionRequest> result = new HashMap<>();
		for (ClassSession session : affected) {
			result.put(session.getId(), new RoomSessionResolutionRequest(session.getId(), inputs.bulk().action(), inputs.bulk().targetRoomId(), null, null, inputs.bulk().reason()));
		}
		for (var override : inputs.explicit().entrySet()) {
			if (!result.containsKey(override.getKey())) {
				throw RoomException.validation("resolutionOverrides", "sessionId must reference an affected session");
			}
			result.put(override.getKey(), override.getValue());
		}
		return result;
	}

	void validateOperation(List<ClassSession> affected, Map<UUID, RoomSessionResolutionRequest> resolutions, Map<UUID, Room> lockedRooms, Impact impact) {
		if (impact.permanentDisable()) {
			List<UUID> inProgress = affected.stream().filter(session -> session.getStatus() == ClassSessionStatus.IN_PROGRESS).map(ClassSession::getId).sorted(Comparator.comparing(UUID::toString)).toList();
			if (!inProgress.isEmpty())
				throw RoomException.inProgressSessionsConflict(inProgress);
		}
		for (ClassSession session : affected) {
			RoomSessionResolutionRequest resolution = resolutions.get(session.getId());
			switch(resolution.action()) {
				case "MOVE" ->
					validateMove(session, resolution, lockedRooms);
				case "RESCHEDULE" ->
					validateReschedule(session, resolution, impact);
				case "CANCEL" ->
					validateCancel(session);
				default ->
					throw RoomException.validation("resolutions.action", "must be MOVE, RESCHEDULE, or CANCEL");
			}
		}
	}

	void validateMove(ClassSession session, RoomSessionResolutionRequest resolution, Map<UUID, Room> lockedRooms) {
		if (session.getStatus() != ClassSessionStatus.SCHEDULED && session.getStatus() != ClassSessionStatus.IN_PROGRESS) {
			throw RoomException.sessionNotScheduled(session.getId());
		}
		Room targetRoom = lockedRooms.get(resolution.targetRoomId());
		if (targetRoom.getStatus() != RoomStatus.ACTIVE)
			throw RoomException.inactiveRoom();
		if (session.getCapacity() > targetRoom.getCapacity())
			throw RoomException.moveCapacityConflict(session.getId(), targetRoom.getId(), session.getStartTime(), session.getEndTime());
		if (rooms.existsSuspensionOverlap(targetRoom.getId(), session.getStartTime(), session.getEndTime())) {
			throw RoomException.moveSuspensionConflict(session.getId(), targetRoom.getId(), session.getStartTime(), session.getEndTime());
		}
		Optional<UUID> conflict = sessions.findConflictIdExcluding(session.getId(), session.getTeachingCoach().getId(), targetRoom.getId(), session.getStartTime(), session.getEndTime());
		if (conflict.isPresent())
			throw RoomException.moveSessionConflict(session.getId(), targetRoom.getId(), session.getStartTime(), session.getEndTime(), conflict.get());
	}

	void validateReschedule(ClassSession session, RoomSessionResolutionRequest resolution, Impact impact) {
		if (session.getStatus() != ClassSessionStatus.SCHEDULED) {
			throw RoomException.sessionNotScheduled(session.getId());
		}
		validateRescheduleTimes(resolution.startTime(), resolution.endTime());
		if (overlaps(impact.startTime(), impact.endTime(), resolution.startTime(), resolution.endTime())) {
			throw RoomException.suspensionConflict();
		}
		if (rooms.existsSuspensionOverlap(session.getRoom().getId(), resolution.startTime(), resolution.endTime())) {
			throw RoomException.suspensionConflict();
		}
	}

	void validateRescheduleTimes(Instant startTime, Instant endTime) {
		if (!startTime.isAfter(clock.instant()))
			throw RoomException.validation("resolutions.startTime", "must be in the future");
		if (!endTime.isAfter(startTime))
			throw RoomException.validation("resolutions.endTime", "must be later than startTime");
		if (Duration.between(startTime, endTime).compareTo(Duration.ofMinutes(30)) < 0) {
			throw RoomException.validation("resolutions.endTime", "a Session cannot be shorter than 30 minutes");
		}
		if (Duration.between(startTime, endTime).compareTo(Duration.ofMinutes(90)) > 0) {
			throw RoomException.validation("resolutions.endTime", "a Session cannot be longer than 90 minutes");
		}
	}

	void validateCancel(ClassSession session) {
		if (session.getStatus() != ClassSessionStatus.SCHEDULED || !session.getStartTime().isAfter(clock.instant())) {
			throw RoomException.cancelNotAllowed(session.getId());
		}
	}

	boolean overlaps(Instant start, Instant end, Instant otherStart, Instant otherEnd) {
		return start.isBefore(otherEnd) && end.isAfter(otherStart);
	}

	void validateResolutionSet(List<ClassSession> affected, Map<UUID, RoomSessionResolutionRequest> resolutions) {
		if (affected.size() != resolutions.size() || affected.stream().anyMatch(session -> !resolutions.containsKey(session.getId()))) {
			throw RoomException.validation("resolutions", "one resolution is required for every affected session");
		}
	}

	String normalizeReason(String reason, boolean required) {
		String value = reason == null ? null : reason.trim();
		if (required && (value == null || value.isEmpty()))
			throw RoomException.validation("reason", "is required");
		if (value != null && value.length() > 1000)
			throw RoomException.validation("reason", "must be at most 1000 characters");
		return value;
	}

	void validateExtensionRequest(RoomSuspensionExtensionRequest request) {
		if (request == null)
			throw RoomException.suspensionValidation("request", "must be a JSON object");
		if (request.expectedEndTime() == null) {
			throw RoomException.suspensionValidation("expectedEndTime", "is required");
		}
		if (request.endTime() == null) {
			throw RoomException.suspensionValidation("endTime", "is required");
		}
		normalizeReason(request.reason(), true);
		if (request.resolutions() == null && request.bulkResolution() == null) {
			throw RoomException.suspensionValidation("resolutions", "resolutions or bulkResolution is required");
		}
	}
}
