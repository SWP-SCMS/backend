package com.scms.backend.scheduling;

import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
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

	RoomSuspensionService(RoomSuspensionRepository suspensions, RoomRepository rooms, AccountRepository accounts,
			AuditEventRepository audits, Clock clock) {
		this(suspensions, rooms, accounts, audits, clock, null, null, null, null, null, null, null);
	}

	@org.springframework.beans.factory.annotation.Autowired
	RoomSuspensionService(RoomSuspensionRepository suspensions, RoomRepository rooms, AccountRepository accounts,
			AuditEventRepository audits, Clock clock, ClassSessionRepository sessions,
			ClassSessionService sessionService, ClassSessionCancellationService cancellationService,
			RoomService roomService, SessionLifecycleService lifecycle, JdbcTemplate jdbc,
			NotificationWriter notifications) {
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
	RoomImpactPreviewResponse previewImpact(UUID managerId, UUID roomId, RoomImpactPreviewRequest request) {
		ensureActiveManager(managerId);
		Impact impact = validateImpact(request, false);
		rooms.findById(roomId).orElseThrow(RoomException::notFound);
		lifecycle.catchUp(clock.instant());
		List<ClassSession> affected = sessions.findRoomImpacts(roomId, impact.startTime(), impact.endTime(),
			impact.permanentDisable(), clock.instant());
		return RoomImpactPreviewResponse.from(affected, bookingCounts(affected));
	}

	@Transactional
	RoomResponse applyImpact(UUID managerId, UUID roomId, RoomImpactPreviewRequest request) {
		ensureActiveManager(managerId);
		Impact impact = validateImpact(request, true);
		Room sourceRoom = rooms.findByIdForUpdate(roomId).orElseThrow(RoomException::notFound);
		Map<UUID, RoomSessionResolutionRequest> resolutions = resolutionMap(request.resolutions());
		Map<UUID, Room> targetRooms = lockTargetRooms(roomId, resolutions.values());
		lifecycle.catchUp(clock.instant());
		List<ClassSession> affected = sessions.findRoomImpactsForUpdate(roomId, impact.startTime(), impact.endTime(),
			impact.permanentDisable(), clock.instant());
		validateResolutionSet(affected, resolutions);
		for (ClassSession session : affected) {
			RoomSessionResolutionRequest resolution = resolutions.get(session.getId());
			switch (resolution.action().trim().toUpperCase(java.util.Locale.ROOT)) {
			case "MOVE" -> {
				sessionService.moveForRoomImpact(managerId, session,
					targetRooms.get(resolution.targetRoomId()), resolution.reason());
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
		if (impact.permanentDisable()) {
			List<UUID> blocking = rooms.findBlockingSessionIds(roomId);
			if (!blocking.isEmpty()) throw RoomException.activeSessionsConflict(blocking);
			roomService.disableAfterImpactResolution(managerId, sourceRoom, impact.reason());
		}
		else {
			createLocked(managerId, sourceRoom,
				new RoomSuspensionCreateRequest(impact.startTime(), impact.endTime(), impact.reason()), true);
		}
		return RoomResponse.from(sourceRoom);
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

	private Impact validateImpact(RoomImpactPreviewRequest request, boolean applying) {
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
		if (applying) resolutionMap(request.resolutions());
		Instant queryStart = request.startTime() == null ? now : request.startTime();
		Instant queryEnd = request.endTime() == null ? now : request.endTime();
		return new Impact(queryStart, queryEnd, request.permanentDisable(), reason);
	}

	private Map<UUID, RoomSessionResolutionRequest> resolutionMap(List<RoomSessionResolutionRequest> requests) {
		Map<UUID, RoomSessionResolutionRequest> result = new HashMap<>();
		for (RoomSessionResolutionRequest request : requests) {
			if (request == null || request.sessionId() == null || request.action() == null
					|| request.action().trim().isEmpty()) {
				throw RoomException.validation("resolutions", "sessionId and action are required");
			}
			if (result.put(request.sessionId(), request) != null) {
				throw RoomException.validation("resolutions", "session IDs must be unique");
			}
			String action = request.action().trim().toUpperCase(java.util.Locale.ROOT);
			if (!Set.of("MOVE", "RESCHEDULE", "CANCEL").contains(action)) {
				throw RoomException.validation("resolutions.action", "must be MOVE, RESCHEDULE, or CANCEL");
			}
			if (action.equals("MOVE") && request.targetRoomId() == null) {
				throw RoomException.validation("resolutions.targetRoomId", "is required for MOVE");
			}
			if (action.equals("RESCHEDULE") && (request.startTime() == null || request.endTime() == null)) {
				throw RoomException.validation("resolutions.startTime", "startTime and endTime are required for RESCHEDULE");
			}
			if ((action.equals("CANCEL") || action.equals("RESCHEDULE"))) normalizeReason(request.reason(), true);
		}
		return result;
	}

	private Map<UUID, Room> lockTargetRooms(UUID sourceRoomId, Collection<RoomSessionResolutionRequest> requests) {
		Set<UUID> targetIds = new TreeSet<>();
		requests.stream().filter(request -> request != null && "MOVE".equalsIgnoreCase(request.action())
			&& request.targetRoomId() != null)
			.forEach(request -> targetIds.add(request.targetRoomId()));
		Map<UUID, Room> result = new HashMap<>();
		for (UUID targetId : targetIds) {
			if (targetId.equals(sourceRoomId)) throw RoomException.validation("resolutions.targetRoomId",
				"must differ from the source room");
			result.put(targetId, rooms.findByIdForUpdate(targetId).orElseThrow(RoomException::notFound));
		}
		return result;
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

	private record Impact(Instant startTime, Instant endTime, boolean permanentDisable, String reason) { }

	private Room findRoom(UUID roomId) {
		return rooms.findById(roomId).orElseThrow(RoomException::notFound);
	}

	private void ensureActiveManager(UUID managerId) {
		if (!accounts.existsByIdAndRoleAndStatus(managerId, AccountRole.MANAGER, AccountStatus.ACTIVE)) {
			throw new InvalidAuthenticatedAccountException();
		}
	}
}
