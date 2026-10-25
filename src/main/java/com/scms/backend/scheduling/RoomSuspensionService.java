package com.scms.backend.scheduling;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.auth.InvalidAuthenticatedAccountException;
import com.scms.backend.audit.AuditEvent;
import com.scms.backend.audit.AuditEventRepository;
import com.scms.backend.notification.NotificationWriter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoomSuspensionService {

	private final RoomSuspensionRepository suspensions;
	private final RoomRepository rooms;
	private final AccountRepository accounts;
	private final AuditEventRepository audits;
	private final Clock clock;
	private final ClassSessionRepository sessions;
	private final ClassSessionService sessionService;
	private final ClassSessionCancellationService cancellationService;
	private final RoomService roomService;
	private final SessionLifecycleService lifecycle;
	private final JdbcTemplate jdbc;
	private final NotificationWriter notifications;
	private final RoomImpactLockRendezvous lockRendezvous;

	RoomSuspensionService(RoomSuspensionRepository suspensions, RoomRepository rooms, AccountRepository accounts,
			AuditEventRepository audits, Clock clock) {
		this(suspensions, rooms, accounts, audits, clock, null, null, null, null, null, null, null, null);
	}

	@org.springframework.beans.factory.annotation.Autowired
	RoomSuspensionService(RoomSuspensionRepository suspensions, RoomRepository rooms, AccountRepository accounts,
			AuditEventRepository audits, Clock clock, ClassSessionRepository sessions,
			ClassSessionService sessionService, ClassSessionCancellationService cancellationService,
			RoomService roomService, SessionLifecycleService lifecycle, JdbcTemplate jdbc,
			NotificationWriter notifications, RoomImpactLockRendezvous lockRendezvous) {
		this.suspensions = suspensions;
		this.rooms = rooms;
		this.accounts = accounts;
		this.audits = audits;
		this.clock = clock;
		this.sessions = sessions;
		this.sessionService = sessionService;
		this.cancellationService = cancellationService;
		this.roomService = roomService;
		this.lifecycle = lifecycle;
		this.jdbc = jdbc;
		this.notifications = notifications;
		this.lockRendezvous = lockRendezvous;
	}

	@Transactional(readOnly = true)
	List<RoomSuspensionResponse> list(UUID managerId, UUID roomId) {
		ensureActiveManager(managerId);
		findRoom(roomId);
		return suspensions.findByRoom_IdOrderByStartTimeAsc(roomId).stream()
			.map(RoomSuspensionResponse::from).toList();
	}

	@Transactional
	RoomSuspensionResponse create(UUID managerId, UUID roomId, RoomSuspensionCreateRequest request) {
		ensureActiveManager(managerId);
		if (request == null) throw RoomException.suspensionValidation("request", "must be a JSON object");
		Room room = rooms.findByIdForUpdate(roomId).orElseThrow(RoomException::notFound);
		return createLocked(managerId, room, request, false);
	}

	@Transactional
	RoomSuspensionResponse extend(UUID managerId, UUID roomId, UUID suspensionId,
			RoomSuspensionExtensionRequest request) {
		validateExtensionRequest(request);
		ensureActiveManager(managerId);
		ResolutionInputs inputs = resolutionInputs(request.resolutions(), request.bulkResolution(),
			request.resolutionOverrides());
		if (lockRendezvous != null) lockRendezvous.await();
		RoomSuspension suspension = suspensions.findByIdForUpdate(suspensionId)
			.filter(candidate -> candidate.getRoom().getId().equals(roomId))
			.orElseThrow(RoomException::notFound);
		if (!suspension.getEndTime().equals(request.expectedEndTime())) throw RoomException.suspensionChanged();
		if (!suspension.getEndTime().isAfter(clock.instant())) throw RoomException.suspensionExpired();
		if (request.endTime() == null || !request.endTime().isAfter(request.expectedEndTime())
				|| !request.endTime().isAfter(suspension.getEndTime())) {
			throw RoomException.suspensionValidation("endTime", "must be later than expectedEndTime and current endTime");
		}
		String reason = normalizeReason(request.reason(), true);
		Map<UUID, Room> lockedRooms = lockRooms(roomId, inputs.lockCandidates());
		if (suspensions.existsOverlapExcluding(roomId, suspensionId, suspension.getStartTime(), request.endTime())) {
			throw RoomException.suspensionConflict();
		}
		Instant currentEndTime = suspension.getEndTime();
		Instant now = clock.instant();
		lifecycle.catchUp(now);
		List<ClassSession> affected = sessions.findRoomImpactsForUpdate(roomId, currentEndTime, request.endTime(),
			false, now);
		Map<UUID, RoomSessionResolutionRequest> resolutions = effectiveResolutions(affected, inputs);
		validateResolutionSet(affected, resolutions);
		validateOperation(affected, resolutions, lockedRooms,
			new Impact(suspension.getStartTime(), request.endTime(), false, reason));
		applyResolutions(managerId, affected, resolutions, lockedRooms);
		Map<String, Object> before = suspensionSnapshot(suspension);
		suspension.extend(request.endTime(), reason);
		try {
			suspensions.saveAndFlush(suspension);
		}
		catch (DataIntegrityViolationException exception) {
			throw RoomException.suspensionConflict();
		}
		Map<String, Object> after = suspensionSnapshot(suspension);
		audits.save(new AuditEvent(UUID.randomUUID(), managerId, "ROOM_SUSPENSION_EXTENDED", "ROOM_SUSPENSION",
			suspensionId, reason, before, after));
		return RoomSuspensionResponse.from(suspension);
	}

	@Transactional
	RoomImpactPreviewResponse previewImpact(UUID managerId, UUID roomId, RoomImpactPreviewRequest request) {
		ensureActiveManager(managerId);
		Impact impact = validateImpact(request);
		rooms.findById(roomId).orElseThrow(RoomException::notFound);
		lifecycle.catchUp(clock.instant());
		List<ClassSession> affected = sessions.findRoomImpacts(roomId, impact.startTime(), impact.endTime(),
			impact.permanentDisable(), clock.instant());
		return RoomImpactPreviewResponse.from(affected, bookingCounts(affected));
	}

	@Transactional
	RoomResponse applyImpact(UUID managerId, UUID roomId, RoomImpactPreviewRequest request) {
		ensureActiveManager(managerId);
		Impact impact = validateImpact(request);
		ResolutionInputs inputs = resolutionInputs(request.resolutions(), request.bulkResolution(),
			request.resolutionOverrides());
		if (lockRendezvous != null) lockRendezvous.await();
		Map<UUID, Room> lockedRooms = lockRooms(roomId, inputs.lockCandidates());
		Room sourceRoom = lockedRooms.get(roomId);
		lifecycle.catchUp(clock.instant());
		List<ClassSession> affected = sessions.findRoomImpactsForUpdate(roomId, impact.startTime(), impact.endTime(),
			impact.permanentDisable(), clock.instant());
		Map<UUID, RoomSessionResolutionRequest> resolutions = effectiveResolutions(affected, inputs);
		validateResolutionSet(affected, resolutions);
		validateOperation(affected, resolutions, lockedRooms, impact);
		applyResolutions(managerId, affected, resolutions, lockedRooms);
		if (impact.permanentDisable()) {
			roomService.disableAfterImpactResolution(managerId, sourceRoom, impact.reason());
		}
		else {
			createLocked(managerId, sourceRoom,
				new RoomSuspensionCreateRequest(impact.startTime(), impact.endTime(), impact.reason()), true);
		}
		return RoomResponse.from(sourceRoom);
	}

	private void applyResolutions(UUID managerId, List<ClassSession> affected,
			Map<UUID, RoomSessionResolutionRequest> resolutions, Map<UUID, Room> lockedRooms) {
		for (ClassSession session : affected) {
			RoomSessionResolutionRequest resolution = resolutions.get(session.getId());
			switch (resolution.action()) {
			case "MOVE" -> {
				sessionService.moveForRoomImpact(managerId, session,
					lockedRooms.get(resolution.targetRoomId()), resolution.reason());
				notifyChangedSession(session, "SESSION_ROOM_CHANGED");
			}
			case "RESCHEDULE" -> {
				sessionService.rescheduleForRoomImpact(managerId, session,
					resolution.startTime(), resolution.endTime(), resolution.reason());
				notifyChangedSession(session, "SESSION_RESCHEDULED");
			}
			case "CANCEL" -> cancellationService.cancel(managerId, session.getId(),
				new ClassSessionCancellationRequest(resolution.reason()));
			default -> throw RoomException.validation("resolutions.action", "must be MOVE, RESCHEDULE, or CANCEL");
			}
		}
	}

	private RoomSuspensionResponse createLocked(UUID managerId, Room room, RoomSuspensionCreateRequest request,
			boolean resolvedImpacts) {
		Instant now = clock.instant();
		if (request.startTime() == null || !request.startTime().isAfter(now)) {
			throw RoomException.suspensionValidation("startTime", "must be in the future");
		}
		if (request.endTime() == null || !request.endTime().isAfter(request.startTime())) {
			throw RoomException.suspensionValidation("endTime", "must be later than startTime");
		}
		String reason = request.reason() == null || request.reason().trim().isEmpty()
			? null : request.reason().trim();
		if (reason != null && reason.length() > 1000) {
			throw RoomException.suspensionValidation("reason", "must be at most 1000 characters");
		}
		if (suspensions.existsOverlap(room.getId(), request.startTime(), request.endTime())
				|| (!resolvedImpacts && rooms.existsActiveSessionOverlap(room.getId(), request.startTime(), request.endTime()))) {
			throw RoomException.suspensionConflict();
		}
		RoomSuspension suspension = new RoomSuspension(UUID.randomUUID(), room, request.startTime(),
			request.endTime(), reason, managerId);
		try {
			suspensions.saveAndFlush(suspension);
		}
		catch (DataIntegrityViolationException exception) {
			throw RoomException.suspensionConflict();
		}
		Map<String, Object> after = new HashMap<>();
		after.put("roomId", room.getId().toString());
		after.put("startTime", request.startTime().toString());
		after.put("endTime", request.endTime().toString());
		after.put("reason", reason);
		audits.save(new AuditEvent(UUID.randomUUID(), managerId, "ROOM_SUSPENDED", "ROOM_SUSPENSION",
			suspension.getId(), reason, null, after));
		return RoomSuspensionResponse.from(suspension);
	}

	private Impact validateImpact(RoomImpactPreviewRequest request) {
		if (request == null) throw RoomException.validation("request", "must be a JSON object");
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

	private ResolutionInputs resolutionInputs(List<RoomSessionResolutionRequest> legacy,
			RoomBulkResolutionRequest bulk, List<RoomSessionResolutionRequest> overrides) {
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
		return new ResolutionInputs(normalizeBulkResolution(bulk),
			resolutionMap(overrideRequests, "resolutionOverrides"));
	}

	private RoomBulkResolutionRequest normalizeBulkResolution(RoomBulkResolutionRequest request) {
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
		if (action.equals("CANCEL")) normalizeReason(request.reason(), true);
		return new RoomBulkResolutionRequest(action, request.targetRoomId(), request.reason());
	}

	private Map<UUID, RoomSessionResolutionRequest> resolutionMap(List<RoomSessionResolutionRequest> requests,
			String field) {
		Map<UUID, RoomSessionResolutionRequest> result = new HashMap<>();
		for (RoomSessionResolutionRequest request : requests) {
			if (request == null || request.sessionId() == null || request.action() == null) {
				throw RoomException.validation(field, "sessionId and action are required");
			}
			String action = request.action().trim().toUpperCase(java.util.Locale.ROOT);
			if (action.isEmpty()) throw RoomException.validation(field, "sessionId and action are required");
			RoomSessionResolutionRequest normalized = new RoomSessionResolutionRequest(request.sessionId(), action,
				request.targetRoomId(), request.startTime(), request.endTime(), request.reason());
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
			if ((action.equals("CANCEL") || action.equals("RESCHEDULE"))) normalizeReason(request.reason(), true);
		}
		return result;
	}

	private Map<UUID, RoomSessionResolutionRequest> effectiveResolutions(List<ClassSession> affected,
			ResolutionInputs inputs) {
		if (inputs.bulk() == null) return inputs.explicit();
		Map<UUID, RoomSessionResolutionRequest> result = new HashMap<>();
		for (ClassSession session : affected) {
			result.put(session.getId(), new RoomSessionResolutionRequest(session.getId(), inputs.bulk().action(),
				inputs.bulk().targetRoomId(), null, null, inputs.bulk().reason()));
		}
		for (var override : inputs.explicit().entrySet()) {
			if (!result.containsKey(override.getKey())) {
				throw RoomException.validation("resolutionOverrides", "sessionId must reference an affected session");
			}
			result.put(override.getKey(), override.getValue());
		}
		return result;
	}

	private Map<UUID, Room> lockRooms(UUID sourceRoomId, Collection<RoomSessionResolutionRequest> requests) {
		Set<UUID> targetIds = new TreeSet<>(Comparator.comparing(UUID::toString));
		requests.stream().filter(request -> request != null && "MOVE".equals(request.action())
			&& request.targetRoomId() != null)
			.forEach(request -> targetIds.add(request.targetRoomId()));
		if (targetIds.contains(sourceRoomId)) throw RoomException.validation("resolutions.targetRoomId",
			"must differ from the source room");
		targetIds.add(sourceRoomId);
		Map<UUID, Room> result = new HashMap<>();
		for (UUID roomId : targetIds) {
			result.put(roomId, rooms.findByIdForUpdate(roomId).orElseThrow(RoomException::notFound));
		}
		return result;
	}

	private void validateOperation(List<ClassSession> affected, Map<UUID, RoomSessionResolutionRequest> resolutions,
			Map<UUID, Room> lockedRooms, Impact impact) {
		if (impact.permanentDisable()) {
			List<UUID> inProgress = affected.stream()
				.filter(session -> session.getStatus() == ClassSessionStatus.IN_PROGRESS)
				.map(ClassSession::getId).sorted(Comparator.comparing(UUID::toString)).toList();
			if (!inProgress.isEmpty()) throw RoomException.inProgressSessionsConflict(inProgress);
		}
		for (ClassSession session : affected) {
			RoomSessionResolutionRequest resolution = resolutions.get(session.getId());
			switch (resolution.action()) {
			case "MOVE" -> validateMove(session, resolution, lockedRooms);
			case "RESCHEDULE" -> validateReschedule(session, resolution, impact);
			case "CANCEL" -> validateCancel(session);
			default -> throw RoomException.validation("resolutions.action", "must be MOVE, RESCHEDULE, or CANCEL");
			}
		}
	}

	private void validateMove(ClassSession session, RoomSessionResolutionRequest resolution,
			Map<UUID, Room> lockedRooms) {
		if (session.getStatus() != ClassSessionStatus.SCHEDULED
				&& session.getStatus() != ClassSessionStatus.IN_PROGRESS) {
			throw RoomException.sessionNotScheduled(session.getId());
		}
		Room targetRoom = lockedRooms.get(resolution.targetRoomId());
		if (targetRoom.getStatus() != RoomStatus.ACTIVE) throw RoomException.inactiveRoom();
		if (session.getCapacity() > targetRoom.getCapacity()) throw RoomException.moveCapacityConflict(
			session.getId(), targetRoom.getId(), session.getStartTime(), session.getEndTime());
		if (rooms.existsSuspensionOverlap(targetRoom.getId(), session.getStartTime(), session.getEndTime())) {
			throw RoomException.moveSuspensionConflict(session.getId(), targetRoom.getId(),
				session.getStartTime(), session.getEndTime());
		}
		Optional<UUID> conflict = sessions.findConflictIdExcluding(session.getId(),
			session.getTeachingCoach().getId(), targetRoom.getId(), session.getStartTime(), session.getEndTime());
		if (conflict.isPresent()) throw RoomException.moveSessionConflict(session.getId(), targetRoom.getId(),
			session.getStartTime(), session.getEndTime(), conflict.get());
	}

	private void validateReschedule(ClassSession session, RoomSessionResolutionRequest resolution, Impact impact) {
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

	private void validateRescheduleTimes(Instant startTime, Instant endTime) {
		if (!startTime.isAfter(clock.instant())) throw RoomException.validation("resolutions.startTime",
			"must be in the future");
		if (!endTime.isAfter(startTime)) throw RoomException.validation("resolutions.endTime",
			"must be later than startTime");
		if (Duration.between(startTime, endTime).compareTo(Duration.ofMinutes(30)) < 0) {
			throw RoomException.validation("resolutions.endTime", "a Session cannot be shorter than 30 minutes");
		}
		if (Duration.between(startTime, endTime).compareTo(Duration.ofMinutes(90)) > 0) {
			throw RoomException.validation("resolutions.endTime", "a Session cannot be longer than 90 minutes");
		}
	}

	private void validateCancel(ClassSession session) {
		if (session.getStatus() != ClassSessionStatus.SCHEDULED || !session.getStartTime().isAfter(clock.instant())) {
			throw RoomException.cancelNotAllowed(session.getId());
		}
	}

	private boolean overlaps(Instant start, Instant end, Instant otherStart, Instant otherEnd) {
		return start.isBefore(otherEnd) && end.isAfter(otherStart);
	}

	private void validateResolutionSet(List<ClassSession> affected,
			Map<UUID, RoomSessionResolutionRequest> resolutions) {
		if (affected.size() != resolutions.size() || affected.stream().anyMatch(session -> !resolutions.containsKey(session.getId()))) {
			throw RoomException.validation("resolutions", "one resolution is required for every affected session");
		}
	}

	private Map<UUID, Long> bookingCounts(List<ClassSession> affected) {
		if (affected.isEmpty()) return Map.of();
		return sessions.countBookedBySessionIds(affected.stream().map(ClassSession::getId).toList()).stream()
			.collect(java.util.stream.Collectors.toMap(ClassSessionRepository.BookingCountView::getSessionId,
				ClassSessionRepository.BookingCountView::getBookedCount));
	}

	private Map<String, Object> suspensionSnapshot(RoomSuspension suspension) {
		Map<String, Object> snapshot = new HashMap<>();
		snapshot.put("roomId", suspension.getRoom().getId().toString());
		snapshot.put("startTime", suspension.getStartTime().toString());
		snapshot.put("endTime", suspension.getEndTime().toString());
		snapshot.put("reason", suspension.getReason());
		return snapshot;
	}

	private void notifyChangedSession(ClassSession session, String notificationType) {
		if (jdbc == null || notifications == null) return;
		Map<String, Object> payload = Map.of(
			"sessionId", session.getId().toString(),
			"roomId", session.getRoom().getId().toString(),
			"startTime", session.getStartTime().toString(),
			"endTime", session.getEndTime().toString());
		String eventPrefix = notificationType + ":" + session.getId();
		notifications.write(eventPrefix + ":COACH", session.getTeachingCoach().getId(), notificationType,
			"CLASS_SESSION", session.getId(), payload);
		jdbc.queryForList("""
			select member_account_id from bookings
			where class_session_id = ? and status = 'BOOKED'
			""", UUID.class, session.getId()).forEach(memberId ->
			notifications.write(eventPrefix + ":MEMBER:" + memberId, memberId, notificationType,
				"CLASS_SESSION", session.getId(), payload));
	}

	private String normalizeReason(String reason, boolean required) {
		String value = reason == null ? null : reason.trim();
		if (required && (value == null || value.isEmpty())) throw RoomException.validation("reason", "is required");
		if (value != null && value.length() > 1000) throw RoomException.validation("reason",
			"must be at most 1000 characters");
		return value;
	}

	private void validateExtensionRequest(RoomSuspensionExtensionRequest request) {
		if (request == null) throw RoomException.suspensionValidation("request", "must be a JSON object");
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

	private record Impact(Instant startTime, Instant endTime, boolean permanentDisable, String reason) { }

	private record ResolutionInputs(RoomBulkResolutionRequest bulk,
			Map<UUID, RoomSessionResolutionRequest> explicit) {
		Collection<RoomSessionResolutionRequest> lockCandidates() {
			List<RoomSessionResolutionRequest> candidates = new ArrayList<>(explicit.values());
			if (bulk != null) candidates.add(new RoomSessionResolutionRequest(null, bulk.action(),
				bulk.targetRoomId(), null, null, bulk.reason()));
			return candidates;
		}
	}

	private Room findRoom(UUID roomId) {
		return rooms.findById(roomId).orElseThrow(RoomException::notFound);
	}

	private void ensureActiveManager(UUID managerId) {
		if (!accounts.existsByIdAndRoleAndStatus(managerId, AccountRole.MANAGER, AccountStatus.ACTIVE)) {
			throw new InvalidAuthenticatedAccountException();
		}
	}
}
