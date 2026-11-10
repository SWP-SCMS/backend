package com.scms.backend.scheduling.room;

import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.audit.AuditEvent;
import com.scms.backend.audit.AuditEventRepository;
import com.scms.backend.auth.InvalidAuthenticatedAccountException;
import com.scms.backend.course.session.CourseSessionRepository;
import com.scms.backend.notification.NotificationWriter;
import com.scms.backend.scheduling.ClassSession;
import com.scms.backend.scheduling.Room;
import com.scms.backend.scheduling.RoomException;
import com.scms.backend.scheduling.RoomSuspension;
import com.scms.backend.scheduling.resource.SharedResourceLockService;
import com.scms.backend.scheduling.session.ClassSessionCancellationRequest;
import com.scms.backend.scheduling.session.ClassSessionCancellationService;
import com.scms.backend.scheduling.session.ClassSessionRepository;
import com.scms.backend.scheduling.session.ClassSessionService;
import com.scms.backend.scheduling.session.SessionLifecycleService;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
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

	private final CourseSessionRepository courseSessions;

	private final SharedResourceLockService resourceLocks;

	RoomSuspensionService(RoomSuspensionRepository suspensions, RoomRepository rooms, AccountRepository accounts, AuditEventRepository audits, Clock clock) {
		this(suspensions, rooms, accounts, audits, clock, null, null, null, null, null, null, null, null, null, null);
	}

	RoomSuspensionService(RoomSuspensionRepository suspensions, RoomRepository rooms, AccountRepository accounts, AuditEventRepository audits, Clock clock, ClassSessionRepository sessions, ClassSessionService sessionService, ClassSessionCancellationService cancellationService, RoomService roomService, SessionLifecycleService lifecycle, JdbcTemplate jdbc, NotificationWriter notifications, RoomImpactLockRendezvous lockRendezvous) {
		this(suspensions, rooms, accounts, audits, clock, sessions, sessionService, cancellationService, roomService, lifecycle, jdbc, notifications, lockRendezvous, null, null);
	}

	@org.springframework.beans.factory.annotation.Autowired
	RoomSuspensionService(RoomSuspensionRepository suspensions, RoomRepository rooms, AccountRepository accounts, AuditEventRepository audits, Clock clock, ClassSessionRepository sessions, ClassSessionService sessionService, ClassSessionCancellationService cancellationService, RoomService roomService, SessionLifecycleService lifecycle, JdbcTemplate jdbc, NotificationWriter notifications, RoomImpactLockRendezvous lockRendezvous, CourseSessionRepository courseSessions, SharedResourceLockService resourceLocks) {
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
		this.courseSessions = courseSessions;
		this.resourceLocks = resourceLocks;
		this.roomAvailabilityAnalysis = new RoomAvailabilityAnalysis(this.rooms, this.sessions, this.roomService);
		this.roomImpactPolicy = new RoomImpactPolicy(this.rooms, this.clock, this.sessions);
		this.roomSuspensionJdbcRepository = new RoomSuspensionJdbcRepository(this.jdbc);
	}

	@Transactional(readOnly = true)
	List<RoomSuspensionResponse> list(UUID managerId, UUID roomId) {
		ensureActiveManager(managerId);
		findRoom(roomId);
		return suspensions.findByRoom_IdOrderByStartTimeAsc(roomId).stream().map(RoomSuspensionResponse::from).toList();
	}

	@Transactional
	RoomSuspensionResponse create(UUID managerId, UUID roomId, RoomSuspensionCreateRequest request) {
		ensureActiveManager(managerId);
		if (request == null)
			throw RoomException.suspensionValidation("request", "must be a JSON object");
		Room room = rooms.findByIdForUpdate(roomId).orElseThrow(RoomException::notFound);
		return createLocked(managerId, room, request, false);
	}

	@Transactional
	RoomSuspensionResponse extend(UUID managerId, UUID roomId, UUID suspensionId, RoomSuspensionExtensionRequest request) {
		validateExtensionRequest(request);
		ensureActiveManager(managerId);
		ResolutionInputs inputs = resolutionInputs(request.resolutions(), request.bulkResolution(), request.resolutionOverrides());
		if (lockRendezvous != null)
			lockRendezvous.await();
		RoomSuspension suspension = suspensions.findByIdForUpdate(suspensionId).filter(candidate -> candidate.getRoom().getId().equals(roomId)).orElseThrow(RoomException::notFound);
		if (!suspension.getEndTime().equals(request.expectedEndTime()))
			throw RoomException.suspensionChanged();
		if (!suspension.getEndTime().isAfter(clock.instant()))
			throw RoomException.suspensionExpired();
		if (request.endTime() == null || !request.endTime().isAfter(request.expectedEndTime()) || !request.endTime().isAfter(suspension.getEndTime())) {
			throw RoomException.suspensionValidation("endTime", "must be later than expectedEndTime and current endTime");
		}
		String reason = normalizeReason(request.reason(), true);
		Set<UUID> lockedCoachIds = lockImpactResources(roomId, inputs.lockCandidates(), sessions.findRoomImpactCoachIds(roomId, suspension.getEndTime(), request.endTime(), false, clock.instant()));
		Map<UUID, Room> lockedRooms = lockRooms(roomId, inputs.lockCandidates());
		if (suspensions.existsOverlapExcluding(roomId, suspensionId, suspension.getStartTime(), request.endTime())) {
			throw RoomException.suspensionConflict();
		}
		Instant currentEndTime = suspension.getEndTime();
		Instant now = clock.instant();
		lifecycle.catchUp(now);
		List<ClassSession> affected = sessions.findRoomImpactsForUpdate(roomId, currentEndTime, request.endTime(), false, now);
		ensureLockedCoaches(affected, lockedCoachIds);
		ensureNoCourseImpacts(roomId, currentEndTime, request.endTime(), false, now);
		Map<UUID, RoomSessionResolutionRequest> resolutions = effectiveResolutions(affected, inputs);
		validateResolutionSet(affected, resolutions);
		validateOperation(affected, resolutions, lockedRooms, new Impact(suspension.getStartTime(), request.endTime(), false, reason));
		applyResolutions(managerId, affected, resolutions, lockedRooms);
		Map<String, Object> before = suspensionSnapshot(suspension);
		suspension.extend(request.endTime(), reason);
		try {
			suspensions.saveAndFlush(suspension);
		} catch (DataIntegrityViolationException exception) {
			throw RoomException.suspensionConflict();
		}
		Map<String, Object> after = suspensionSnapshot(suspension);
		audits.save(new AuditEvent(UUID.randomUUID(), managerId, "ROOM_SUSPENSION_EXTENDED", "ROOM_SUSPENSION", suspensionId, reason, before, after));
		return RoomSuspensionResponse.from(suspension);
	}

	@Transactional
	public RoomImpactPreviewResponse previewImpact(UUID managerId, UUID roomId, RoomImpactPreviewRequest request) {
		ensureActiveManager(managerId);
		Impact impact = validateImpact(request);
		rooms.findById(roomId).orElseThrow(RoomException::notFound);
		lifecycle.catchUp(clock.instant());
		List<ClassSession> affected = sessions.findRoomImpacts(roomId, impact.startTime(), impact.endTime(), impact.permanentDisable(), clock.instant());
		List<UUID> courseImpacts = courseImpactIds(roomId, impact.startTime(), impact.endTime(), impact.permanentDisable(), clock.instant());
		TargetRoomAvailability availability = targetRoomAvailability(roomId, affected);
		return RoomImpactPreviewResponse.from(affected, bookingCounts(affected), availability.bySession(), availability.bulk(), courseImpacts);
	}

	private TargetRoomAvailability targetRoomAvailability(UUID sourceRoomId, List<ClassSession> affected) {
		return roomAvailabilityAnalysis.targetRoomAvailability(sourceRoomId, affected);
	}

	@Transactional
	public RoomResponse applyImpact(UUID managerId, UUID roomId, RoomImpactPreviewRequest request) {
		ensureActiveManager(managerId);
		Impact impact = validateImpact(request);
		ResolutionInputs inputs = resolutionInputs(request.resolutions(), request.bulkResolution(), request.resolutionOverrides());
		if (lockRendezvous != null)
			lockRendezvous.await();
		Set<UUID> lockedCoachIds = lockImpactResources(roomId, inputs.lockCandidates(), sessions.findRoomImpactCoachIds(roomId, impact.startTime(), impact.endTime(), impact.permanentDisable(), clock.instant()));
		Map<UUID, Room> lockedRooms = lockRooms(roomId, inputs.lockCandidates());
		Room sourceRoom = lockedRooms.get(roomId);
		lifecycle.catchUp(clock.instant());
		List<ClassSession> affected = sessions.findRoomImpactsForUpdate(roomId, impact.startTime(), impact.endTime(), impact.permanentDisable(), clock.instant());
		ensureLockedCoaches(affected, lockedCoachIds);
		ensureNoCourseImpacts(roomId, impact.startTime(), impact.endTime(), impact.permanentDisable(), clock.instant());
		Map<UUID, RoomSessionResolutionRequest> resolutions = effectiveResolutions(affected, inputs);
		validateResolutionSet(affected, resolutions);
		validateOperation(affected, resolutions, lockedRooms, impact);
		applyResolutions(managerId, affected, resolutions, lockedRooms);
		if (impact.permanentDisable()) {
			roomService.disableAfterImpactResolution(managerId, sourceRoom, impact.reason());
		} else {
			createLocked(managerId, sourceRoom, new RoomSuspensionCreateRequest(impact.startTime(), impact.endTime(), impact.reason()), true);
		}
		return RoomResponse.from(sourceRoom);
	}

	private void applyResolutions(UUID managerId, List<ClassSession> affected, Map<UUID, RoomSessionResolutionRequest> resolutions, Map<UUID, Room> lockedRooms) {
		for (ClassSession session : affected) {
			RoomSessionResolutionRequest resolution = resolutions.get(session.getId());
			switch(resolution.action()) {
				case "MOVE" ->
					{
						sessionService.moveForRoomImpact(managerId, session, lockedRooms.get(resolution.targetRoomId()), resolution.reason());
						notifyChangedSession(session, "SESSION_ROOM_CHANGED");
					}
				case "RESCHEDULE" ->
					{
						sessionService.rescheduleForRoomImpact(managerId, session, resolution.startTime(), resolution.endTime(), resolution.reason());
						notifyChangedSession(session, "SESSION_RESCHEDULED");
					}
				case "CANCEL" ->
					cancellationService.cancel(managerId, session.getId(), new ClassSessionCancellationRequest(resolution.reason()));
				default ->
					throw RoomException.validation("resolutions.action", "must be MOVE, RESCHEDULE, or CANCEL");
			}
		}
	}

	private RoomSuspensionResponse createLocked(UUID managerId, Room room, RoomSuspensionCreateRequest request, boolean resolvedImpacts) {
		Instant now = clock.instant();
		if (request.startTime() == null || !request.startTime().isAfter(now)) {
			throw RoomException.suspensionValidation("startTime", "must be in the future");
		}
		if (request.endTime() == null || !request.endTime().isAfter(request.startTime())) {
			throw RoomException.suspensionValidation("endTime", "must be later than startTime");
		}
		String reason = request.reason() == null || request.reason().trim().isEmpty() ? null : request.reason().trim();
		if (reason != null && reason.length() > 1000) {
			throw RoomException.suspensionValidation("reason", "must be at most 1000 characters");
		}
		if (suspensions.existsOverlap(room.getId(), request.startTime(), request.endTime()) || (!resolvedImpacts && (rooms.existsActiveSessionOverlap(room.getId(), request.startTime(), request.endTime()) || !courseImpactIds(room.getId(), request.startTime(), request.endTime(), false, now).isEmpty()))) {
			throw RoomException.suspensionConflict();
		}
		RoomSuspension suspension = new RoomSuspension(UUID.randomUUID(), room, request.startTime(), request.endTime(), reason, managerId);
		try {
			suspensions.saveAndFlush(suspension);
		} catch (DataIntegrityViolationException exception) {
			throw RoomException.suspensionConflict();
		}
		Map<String, Object> after = new HashMap<>();
		after.put("roomId", room.getId().toString());
		after.put("startTime", request.startTime().toString());
		after.put("endTime", request.endTime().toString());
		after.put("reason", reason);
		audits.save(new AuditEvent(UUID.randomUUID(), managerId, "ROOM_SUSPENDED", "ROOM_SUSPENSION", suspension.getId(), reason, null, after));
		return RoomSuspensionResponse.from(suspension);
	}

	private List<UUID> courseImpactIds(UUID roomId, Instant startTime, Instant endTime, boolean permanentDisable, Instant now) {
		return courseSessions == null ? List.of() : courseSessions.findRoomImpactIds(roomId, startTime, endTime, permanentDisable, now);
	}

	private void ensureNoCourseImpacts(UUID roomId, Instant startTime, Instant endTime, boolean permanentDisable, Instant now) {
		if (!courseImpactIds(roomId, startTime, endTime, permanentDisable, now).isEmpty()) {
			throw RoomException.suspensionConflict();
		}
	}

	private Impact validateImpact(RoomImpactPreviewRequest request) {
		return roomImpactPolicy.validateImpact(request);
	}

	private ResolutionInputs resolutionInputs(List<RoomSessionResolutionRequest> legacy, RoomBulkResolutionRequest bulk, List<RoomSessionResolutionRequest> overrides) {
		return roomImpactPolicy.resolutionInputs(legacy, bulk, overrides);
	}

	private Map<UUID, RoomSessionResolutionRequest> effectiveResolutions(List<ClassSession> affected, ResolutionInputs inputs) {
		return roomImpactPolicy.effectiveResolutions(affected, inputs);
	}

	private Map<UUID, Room> lockRooms(UUID sourceRoomId, Collection<RoomSessionResolutionRequest> requests) {
		Set<UUID> targetIds = impactRoomIds(sourceRoomId, requests);
		Map<UUID, Room> result = new HashMap<>();
		for (UUID roomId : targetIds) {
			result.put(roomId, rooms.findByIdForUpdate(roomId).orElseThrow(RoomException::notFound));
		}
		return result;
	}

	private Set<UUID> lockImpactResources(UUID sourceRoomId, Collection<RoomSessionResolutionRequest> requests, Collection<UUID> affectedCoachIds) {
		if (resourceLocks == null)
			return Set.of();
		Set<UUID> coachIds = Set.copyOf(affectedCoachIds);
		resourceLocks.lock(coachIds, impactRoomIds(sourceRoomId, requests));
		return coachIds;
	}

	private void ensureLockedCoaches(List<ClassSession> affected, Set<UUID> lockedCoachIds) {
		if (resourceLocks == null)
			return;
		if (affected.stream().map(session -> session.getTeachingCoach().getId()).anyMatch(coachId -> !lockedCoachIds.contains(coachId)))
			throw RoomException.suspensionConflict();
	}

	private Set<UUID> impactRoomIds(UUID sourceRoomId, Collection<RoomSessionResolutionRequest> requests) {
		Set<UUID> targetIds = new TreeSet<>(Comparator.comparing(UUID::toString));
		requests.stream().filter(request -> request != null && "MOVE".equals(request.action()) && request.targetRoomId() != null).forEach(request -> targetIds.add(request.targetRoomId()));
		if (targetIds.contains(sourceRoomId))
			throw RoomException.validation("resolutions.targetRoomId", "must differ from the source room");
		targetIds.add(sourceRoomId);
		return targetIds;
	}

	private void validateOperation(List<ClassSession> affected, Map<UUID, RoomSessionResolutionRequest> resolutions, Map<UUID, Room> lockedRooms, Impact impact) {
		roomImpactPolicy.validateOperation(affected, resolutions, lockedRooms, impact);
	}

	private void validateResolutionSet(List<ClassSession> affected, Map<UUID, RoomSessionResolutionRequest> resolutions) {
		roomImpactPolicy.validateResolutionSet(affected, resolutions);
	}

	private Map<UUID, Long> bookingCounts(List<ClassSession> affected) {
		if (affected.isEmpty())
			return Map.of();
		return sessions.countBookedBySessionIds(affected.stream().map(ClassSession::getId).toList()).stream().collect(java.util.stream.Collectors.toMap(ClassSessionRepository.BookingCountView::getSessionId, ClassSessionRepository.BookingCountView::getBookedCount));
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
		if (jdbc == null || notifications == null)
			return;
		Map<String, Object> payload = Map.of("sessionId", session.getId().toString(), "roomId", session.getRoom().getId().toString(), "startTime", session.getStartTime().toString(), "endTime", session.getEndTime().toString());
		String eventPrefix = notificationType + ":" + session.getId();
		notifications.write(eventPrefix + ":COACH", session.getTeachingCoach().getId(), notificationType, "CLASS_SESSION", session.getId(), payload);
		roomSuspensionJdbcRepository.notifyChangedSessionFindBookings(session.getId()).forEach(memberId -> notifications.write(eventPrefix + ":MEMBER:" + memberId, memberId, notificationType, "CLASS_SESSION", session.getId(), payload));
	}

	private String normalizeReason(String reason, boolean required) {
		return roomImpactPolicy.normalizeReason(reason, required);
	}

	private void validateExtensionRequest(RoomSuspensionExtensionRequest request) {
		roomImpactPolicy.validateExtensionRequest(request);
	}

	record Impact(Instant startTime, Instant endTime, boolean permanentDisable, String reason) {
	}

	record TargetRoomAvailability(Map<UUID, List<RoomImpactPreviewResponse.TargetRoom>> bySession, List<RoomImpactPreviewResponse.TargetRoom> bulk) {
	}

	record CapacityPoint(Instant time, int capacity) {
	}

	record ResolutionInputs(RoomBulkResolutionRequest bulk, Map<UUID, RoomSessionResolutionRequest> explicit) {

		Collection<RoomSessionResolutionRequest> lockCandidates() {
			List<RoomSessionResolutionRequest> candidates = new ArrayList<>(explicit.values());
			if (bulk != null)
				candidates.add(new RoomSessionResolutionRequest(null, bulk.action(), bulk.targetRoomId(), null, null, bulk.reason()));
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

	private final RoomAvailabilityAnalysis roomAvailabilityAnalysis;

	private final RoomImpactPolicy roomImpactPolicy;

	private final RoomSuspensionJdbcRepository roomSuspensionJdbcRepository;
}
