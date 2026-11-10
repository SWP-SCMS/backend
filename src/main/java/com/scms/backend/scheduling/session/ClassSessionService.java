package com.scms.backend.scheduling.session;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.audit.AuditEvent;
import com.scms.backend.audit.AuditEventRepository;
import com.scms.backend.auth.InvalidAuthenticatedAccountException;
import com.scms.backend.course.session.CourseSessionRepository;
import com.scms.backend.notification.NotificationWriter;
import com.scms.backend.scheduling.Booking;
import com.scms.backend.scheduling.ClassSession;
import com.scms.backend.scheduling.ClassSessionStatus;
import com.scms.backend.scheduling.PtSessionAssignmentException;
import com.scms.backend.scheduling.RecurringSchedule;
import com.scms.backend.scheduling.RecurringScheduleException;
import com.scms.backend.scheduling.Room;
import com.scms.backend.scheduling.RoomStatus;
import com.scms.backend.scheduling.SessionWaitlist;
import com.scms.backend.scheduling.SportClass;
import com.scms.backend.scheduling.SportClassType;
import com.scms.backend.scheduling.booking.BookingRepository;
import com.scms.backend.scheduling.coachleave.CoachLeaveRepository;
import com.scms.backend.scheduling.resource.SharedResourceLockService;
import com.scms.backend.scheduling.room.RoomImpactLockRendezvous;
import com.scms.backend.scheduling.room.RoomRepository;
import com.scms.backend.scheduling.sportclass.SportClassRepository;
import com.scms.backend.scheduling.waitlist.SessionWaitlistRepository;
import com.scms.backend.training.CoachAssignmentStatus;
import com.scms.backend.training.assignment.CoachAssignmentRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClassSessionService {

	private final ClassSessionRepository sessions;

	private final SportClassRepository classes;

	private final RoomRepository rooms;

	private final AccountRepository accounts;

	private final AuditEventRepository audits;

	private final SessionLifecycleService lifecycle;

	private final Clock clock;

	private final RoomImpactLockRendezvous lockRendezvous;

	private final CoachLeaveRepository coachLeaves;

	private final CoachAssignmentRepository coachAssignments;

	private final NotificationWriter notifications;

	private final BookingRepository bookings;

	private final SessionWaitlistRepository waitlists;

	private final CourseSessionRepository courseSessions;

	private final SharedResourceLockService resourceLocks;

	ClassSessionService(ClassSessionRepository sessions, SportClassRepository classes, RoomRepository rooms, AccountRepository accounts, AuditEventRepository audits, SessionLifecycleService lifecycle, Clock clock) {
		this(sessions, classes, rooms, accounts, audits, lifecycle, clock, null, null, null, null, null, null, null, null);
	}

	ClassSessionService(ClassSessionRepository sessions, SportClassRepository classes, RoomRepository rooms, AccountRepository accounts, AuditEventRepository audits, SessionLifecycleService lifecycle, Clock clock, RoomImpactLockRendezvous lockRendezvous, CoachLeaveRepository coachLeaves, CoachAssignmentRepository coachAssignments, NotificationWriter notifications, BookingRepository bookings, SessionWaitlistRepository waitlists) {
		this(sessions, classes, rooms, accounts, audits, lifecycle, clock, lockRendezvous, coachLeaves, coachAssignments, notifications, bookings, waitlists, null, null);
	}

	@org.springframework.beans.factory.annotation.Autowired
	ClassSessionService(ClassSessionRepository sessions, SportClassRepository classes, RoomRepository rooms, AccountRepository accounts, AuditEventRepository audits, SessionLifecycleService lifecycle, Clock clock, RoomImpactLockRendezvous lockRendezvous, CoachLeaveRepository coachLeaves, CoachAssignmentRepository coachAssignments, NotificationWriter notifications, BookingRepository bookings, SessionWaitlistRepository waitlists, CourseSessionRepository courseSessions, SharedResourceLockService resourceLocks) {
		this.sessions = sessions;
		this.classes = classes;
		this.rooms = rooms;
		this.accounts = accounts;
		this.audits = audits;
		this.lifecycle = lifecycle;
		this.clock = clock;
		this.lockRendezvous = lockRendezvous;
		this.coachLeaves = coachLeaves;
		this.coachAssignments = coachAssignments;
		this.notifications = notifications;
		this.bookings = bookings;
		this.waitlists = waitlists;
		this.courseSessions = courseSessions;
		this.resourceLocks = resourceLocks;
		this.classSessionCatalog = new ClassSessionCatalog(this.sessions, this.lifecycle, this.clock);
		this.classSessionResources = new ClassSessionResources(this.sessions, this.classes, this.rooms, this.accounts, this.clock, this.coachLeaves, this.courseSessions, this.resourceLocks);
	}

	@Transactional
	public ClassSessionResponse create(UUID managerId, ClassSessionCreateRequest request) {
		ensureActiveManager(managerId);
		if (request == null)
			throw RecurringScheduleException.validation("request", "must be a JSON object");
		validateTimes(request.startTime(), request.endTime());
		Resources resources = validateResources(request.classId(), request.coachId(), request.roomId(), request.capacity());
		ensureAvailable(resources, request.startTime(), request.endTime(), request.capacity(), null);
		ClassSession session = newScheduled(resources, null, request.startTime(), request.endTime(), request.capacity(), managerId);
		try {
			sessions.saveAndFlush(session);
		} catch (DataIntegrityViolationException exception) {
			throw RecurringScheduleException.sessionConflict();
		}
		return ClassSessionResponse.from(session);
	}

	@Transactional
	ClassSessionPageResponse list(UUID managerId, Instant from, Instant to, UUID classId, UUID coachId, UUID roomId, ClassSessionStatus status, Pageable pageable) {
		ensureActiveManager(managerId);
		if (from != null && to != null && !to.isAfter(from)) {
			throw RecurringScheduleException.validation("to", "must be later than from");
		}
		advanceStatuses();
		Specification<ClassSession> filters = (root, ignored, builder) -> builder.conjunction();
		if (from != null)
			filters = filters.and((root, ignored, builder) -> builder.greaterThan(root.get("endTime"), from));
		if (to != null)
			filters = filters.and((root, ignored, builder) -> builder.lessThan(root.get("startTime"), to));
		if (classId != null)
			filters = filters.and((root, ignored, builder) -> builder.equal(root.get("sportClass").get("id"), classId));
		if (coachId != null)
			filters = filters.and((root, ignored, builder) -> builder.equal(root.get("teachingCoach").get("id"), coachId));
		if (roomId != null)
			filters = filters.and((root, ignored, builder) -> builder.equal(root.get("room").get("id"), roomId));
		if (status != null)
			filters = filters.and((root, ignored, builder) -> builder.equal(root.get("status"), status));
		var page = sessions.findAll(filters, pageable);
		Map<UUID, Long> counts = new java.util.HashMap<>();
		if (!page.isEmpty())
			sessions.countBookedBySessionIds(page.getContent().stream().map(ClassSession::getId).toList()).forEach(row -> counts.put(row.getSessionId(), row.getBookedCount()));
		return new ClassSessionPageResponse(page.getContent().stream().map(s -> ClassSessionResponse.from(s).withBookedCount(counts.getOrDefault(s.getId(), 0L))).toList(), page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
	}

	@Transactional
	MemberClassSessionPageResponse memberSchedule(UUID memberId, Instant from, Instant to, UUID classId, Pageable pageable) {
		if (!accounts.existsByIdAndRoleAndStatus(memberId, AccountRole.MEMBER, AccountStatus.ACTIVE)) {
			throw new InvalidAuthenticatedAccountException();
		}
		return futureSchedule(from, to, classId, pageable);
	}

	@Transactional
	MemberClassSessionPageResponse receptionSchedule(UUID receptionistId, Instant from, Instant to, UUID classId, Pageable pageable) {
		if (!accounts.existsByIdAndRoleAndStatus(receptionistId, AccountRole.RECEPTIONIST, AccountStatus.ACTIVE)) {
			throw new InvalidAuthenticatedAccountException();
		}
		return futureSchedule(from, to, classId, pageable);
	}

	private MemberClassSessionPageResponse futureSchedule(Instant from, Instant to, UUID classId, Pageable pageable) {
		return futureCatalogue(from, to, classId, null, false, pageable);
	}

	@Transactional
	MemberClassSessionPageResponse memberPtSchedule(UUID memberId, Instant from, Instant to, Pageable pageable) {
		if (!accounts.existsByIdAndRoleAndStatus(memberId, AccountRole.MEMBER, AccountStatus.ACTIVE)) {
			throw new InvalidAuthenticatedAccountException();
		}
		UUID coachId = coachAssignments == null ? null : coachAssignments.findByMemberAccountIdAndStatus(memberId, CoachAssignmentStatus.ACTIVE).map(assignment -> assignment.getCoachAccountId()).orElse(null);
		if (coachId == null)
			return emptyCatalogue(pageable);
		return futureCatalogue(from, to, null, coachId, true, pageable);
	}

	@Transactional
	MemberClassSessionPageResponse staffPtSchedule(UUID staffId, AccountRole role, Instant from, Instant to, Pageable pageable) {
		if (!accounts.existsByIdAndRoleAndStatus(staffId, role, AccountStatus.ACTIVE)) {
			throw new InvalidAuthenticatedAccountException();
		}
		return futureCatalogue(from, to, null, null, true, pageable);
	}

	private MemberClassSessionPageResponse futureCatalogue(Instant from, Instant to, UUID classId, UUID coachId, boolean ptOnly, Pageable pageable) {
		return classSessionCatalog.futureCatalogue(from, to, classId, coachId, ptOnly, pageable);
	}

	private MemberClassSessionPageResponse emptyCatalogue(Pageable pageable) {
		return classSessionCatalog.emptyCatalogue(pageable);
	}

	@Transactional
	ClassSessionDetailResponse get(UUID managerId, UUID sessionId) {
		ensureActiveManager(managerId);
		advanceStatuses();
		ClassSession session = sessions.findById(sessionId).orElseThrow(() -> RecurringScheduleException.notFound("session"));
		return ClassSessionDetailResponse.from(session);
	}

	@Transactional(readOnly = true)
	public List<AvailableCoachResponse> availableCoaches(UUID managerId, UUID sessionId) {
		ensureActiveManager(managerId);
		ClassSession session = sessions.findById(sessionId).orElseThrow(() -> RecurringScheduleException.notFound("session"));
		return sessions.findAvailableCoaches(sessionId, session.getStartTime(), session.getEndTime()).stream().map(AvailableCoachResponse::from).toList();
	}

	@Transactional(readOnly = true)
	public List<AvailableCoachResponse> availableCoaches(UUID managerId, List<Instant> startTimes, List<Instant> endTimes, UUID memberAccountId) {
		ensureActiveManager(managerId);
		if (startTimes == null || endTimes == null || startTimes.isEmpty()) {
			throw RecurringScheduleException.validation("startTime", "at least one time window is required");
		}
		if (startTimes.size() != endTimes.size()) {
			throw RecurringScheduleException.validation("endTime", "must contain one value per startTime");
		}
		if (startTimes.size() > 100) {
			throw RecurringScheduleException.validation("startTime", "at most 100 time windows are allowed");
		}
		for (int index = 0; index < startTimes.size(); index++) {
			validateTimes(startTimes.get(index), endTimes.get(index));
		}
		Map<UUID, AvailableCoachResponse> available = new java.util.LinkedHashMap<>();
		for (AvailableCoachResponse coach : sessions.findAvailableCoachesForWindow(startTimes.getFirst(), endTimes.getFirst()).stream().map(AvailableCoachResponse::from).toList()) {
			available.put(coach.coachAccountId(), coach);
		}
		for (int index = 1; index < startTimes.size() && !available.isEmpty(); index++) {
			Set<UUID> windowCoachIds = sessions.findAvailableCoachesForWindow(startTimes.get(index), endTimes.get(index)).stream().map(ClassSessionRepository.AvailableCoachView::getCoachAccountId).collect(Collectors.toSet());
			available.keySet().retainAll(windowCoachIds);
		}
		if (memberAccountId != null) {
			if (coachAssignments == null)
				return List.of();
			Optional<UUID> assignedCoachId = coachAssignments.findByMemberAccountIdAndStatus(memberAccountId, CoachAssignmentStatus.ACTIVE).map(assignment -> assignment.getCoachAccountId());
			if (assignedCoachId.isEmpty())
				return List.of();
			available.keySet().retainAll(Set.of(assignedCoachId.get()));
		}
		return List.copyOf(available.values());
	}

	@Transactional
	public ClassSessionDetailResponse updateAssignment(UUID managerId, UUID sessionId, ClassSessionAssignmentRequest request) {
		ensureActiveManager(managerId);
		if (request == null || (request.coachId() == null && request.roomId() == null)) {
			throw RecurringScheduleException.validation("request", "coachId or roomId is required");
		}
		advanceStatuses();
		UUID sourceRoomId;
		UUID sourceCoachId = null;
		if (resourceLocks == null) {
			sourceRoomId = sessions.findRoomIdById(sessionId).orElseThrow(() -> RecurringScheduleException.notFound("session"));
			lockAssignmentRooms(sourceRoomId, request.roomId());
		} else {
			ClassSession resourceSnapshot = sessions.findById(sessionId).orElseThrow(() -> RecurringScheduleException.notFound("session"));
			sourceRoomId = resourceSnapshot.getRoom() == null ? null : resourceSnapshot.getRoom().getId();
			sourceCoachId = resourceSnapshot.getTeachingCoach().getId();
			resourceLocks.lock(request.coachId() == null ? List.of(sourceCoachId) : List.of(sourceCoachId, request.coachId()), request.roomId() == null ? (sourceRoomId == null ? List.of() : List.of(sourceRoomId)) : (sourceRoomId == null ? List.of(request.roomId()) : List.of(sourceRoomId, request.roomId())));
		}
		if (lockRendezvous != null)
			lockRendezvous.await();
		ClassSession session = sessions.findByIdForUpdate(sessionId).orElseThrow(() -> RecurringScheduleException.notFound("session"));
		if (resourceLocks != null && (!session.getTeachingCoach().getId().equals(sourceCoachId) || !java.util.Objects.equals(session.getRoom() == null ? null : session.getRoom().getId(), sourceRoomId))) {
			throw RecurringScheduleException.sessionConflict();
		}
		if (session.getStatus() != ClassSessionStatus.SCHEDULED) {
			throw RecurringScheduleException.sessionNotScheduled();
		}
		if (request.roomId() == null && !java.util.Objects.equals(session.getRoom() == null ? null : session.getRoom().getId(), sourceRoomId)) {
			throw RecurringScheduleException.sessionConflict();
		}
		UUID coachId = request.coachId() == null ? session.getTeachingCoach().getId() : request.coachId();
		UUID roomId = request.roomId() == null ? sourceRoomId : request.roomId();
		if (request.expectedVersion() != null && !request.expectedVersion().equals(session.getVersion())) {
			throw RecurringScheduleException.versionConflict();
		}
		UUID beforeCoachId = session.getTeachingCoach().getId();
		UUID beforeRoomId = session.getRoom() == null ? null : session.getRoom().getId();
		if (coachId.equals(beforeCoachId) && java.util.Objects.equals(roomId, beforeRoomId)) {
			return ClassSessionDetailResponse.from(session);
		}
		Resources resources = validateResources(session.getSportClass().getId(), coachId, roomId, session.getCapacity());
		ensureAvailable(resources, session.getStartTime(), session.getEndTime(), session.getCapacity(), sessionId);
		ensurePtMembersCompatible(session, beforeCoachId, coachId);
		session.updateAssignment(resources.coach(), resources.room());
		try {
			sessions.saveAndFlush(session);
		} catch (DataIntegrityViolationException exception) {
			throw RecurringScheduleException.sessionConflict();
		}
		Map<String, Object> before = new java.util.HashMap<>(), after = new java.util.HashMap<>();
		before.put("coachId", beforeCoachId.toString());
		before.put("roomId", beforeRoomId == null ? null : beforeRoomId.toString());
		after.put("coachId", coachId.toString());
		after.put("roomId", roomId == null ? null : roomId.toString());
		audits.save(new AuditEvent(UUID.randomUUID(), managerId, "SESSION_ASSIGNMENT_CHANGED", "CLASS_SESSION", sessionId, "Manager changed session assignment", before, after));
		return ClassSessionDetailResponse.from(session);
	}

	private void ensurePtMembersCompatible(ClassSession session, UUID currentCoachId, UUID requestedCoachId) {
		if (session.getSportClass().getClassType() != SportClassType.PT_1_1 || requestedCoachId.equals(currentCoachId)) {
			return;
		}
		List<Booking> activeBookings = bookings.findBookedBySessionForUpdate(session.getId());
		List<SessionWaitlist> activeWaitlists = waitlists.findWaitingBySessionForUpdate(session.getId());
		Set<UUID> memberIds = new LinkedHashSet<>();
		activeBookings.forEach(booking -> memberIds.add(booking.getMemberAccountId()));
		activeWaitlists.forEach(entry -> memberIds.add(entry.getMemberId()));
		if (memberIds.isEmpty())
			return;
		Set<UUID> compatibleMemberIds = coachAssignments.findActiveForMembersAndCoachForUpdate(memberIds, requestedCoachId).stream().map(assignment -> assignment.getMemberAccountId()).collect(Collectors.toSet());
		Map<UUID, AccountRepository.MemberIdentityView> identities = new java.util.HashMap<>();
		List<PtSessionAffectedBooking> affectedBookings = activeBookings.stream().filter(booking -> !compatibleMemberIds.contains(booking.getMemberAccountId())).map(booking -> {
			var identity = memberIdentity(booking.getMemberAccountId(), identities);
			return new PtSessionAffectedBooking(booking.getId(), booking.getMemberAccountId(), identity.getMemberCode(), identity.getFullName());
		}).toList();
		List<PtSessionAffectedWaitlist> affectedWaitlists = activeWaitlists.stream().filter(entry -> !compatibleMemberIds.contains(entry.getMemberId())).map(entry -> {
			var identity = memberIdentity(entry.getMemberId(), identities);
			return new PtSessionAffectedWaitlist(entry.getId(), entry.getMemberId(), identity.getMemberCode(), identity.getFullName());
		}).toList();
		if (!affectedBookings.isEmpty() || !affectedWaitlists.isEmpty()) {
			throw PtSessionAssignmentException.incompatibleMembers(affectedBookings, affectedWaitlists);
		}
	}

	private AccountRepository.MemberIdentityView memberIdentity(UUID memberId, Map<UUID, AccountRepository.MemberIdentityView> identities) {
		return identities.computeIfAbsent(memberId, id -> accounts.findMemberIdentity(id).orElseThrow(() -> RecurringScheduleException.notFound("member")));
	}

	private void advanceStatuses() {
		classSessionCatalog.advanceStatuses();
	}

	void ensureActiveManager(UUID managerId) {
		if (!accounts.existsByIdAndRoleAndStatus(managerId, AccountRole.MANAGER, AccountStatus.ACTIVE)) {
			throw new InvalidAuthenticatedAccountException();
		}
	}

	void validateTimes(Instant startTime, Instant endTime) {
		classSessionResources.validateTimes(startTime, endTime);
	}

	Resources validateResources(UUID classId, UUID coachId, UUID roomId, Integer capacity) {
		return classSessionResources.validateResources(classId, coachId, roomId, capacity);
	}

	private void lockAssignmentRooms(UUID sourceRoomId, UUID requestedRoomId) {
		java.util.Set<UUID> roomIds = new java.util.TreeSet<>(java.util.Comparator.comparing(UUID::toString));
		roomIds.add(sourceRoomId);
		if (requestedRoomId != null)
			roomIds.add(requestedRoomId);
		for (UUID roomId : roomIds) rooms.findByIdForUpdate(roomId).orElseThrow(() -> RecurringScheduleException.notFound("room"));
	}

	void ensureAvailable(Resources resources, Instant startTime, Instant endTime, int requestedCapacity, UUID excludeSessionId) {
		classSessionResources.ensureAvailable(resources, startTime, endTime, requestedCapacity, excludeSessionId);
	}

	@Transactional
	ClassSessionDetailResponse reschedule(UUID managerId, UUID sessionId, ClassSessionScheduleRequest request) {
		ensureActiveManager(managerId);
		if (request == null)
			throw RecurringScheduleException.validation("request", "must be a JSON object");
		if (request.expectedVersion() == null)
			throw RecurringScheduleException.validation("expectedVersion", "is required");
		validateTimes(request.startTime(), request.endTime());
		String reason = request.reason() == null ? "" : request.reason().trim();
		if (reason.isEmpty())
			throw RecurringScheduleException.validation("reason", "is required");
		if (reason.length() > 1000)
			throw RecurringScheduleException.validation("reason", "must be at most 1000 characters");
		advanceStatuses();
		ClassSession resourceSnapshot = resourceLocks == null ? null : sessions.findById(sessionId).orElseThrow(() -> RecurringScheduleException.notFound("session"));
		UUID currentRoomLockId = resourceSnapshot == null || resourceSnapshot.getRoom() == null ? null : resourceSnapshot.getRoom().getId();
		UUID currentCoachLockId = resourceSnapshot == null ? null : resourceSnapshot.getTeachingCoach().getId();
		if (resourceLocks != null)
			resourceLocks.lock(request.coachId() == null ? List.of(currentCoachLockId) : List.of(currentCoachLockId, request.coachId()), request.roomId() == null ? (currentRoomLockId == null ? List.of() : List.of(currentRoomLockId)) : (currentRoomLockId == null ? List.of(request.roomId()) : List.of(currentRoomLockId, request.roomId())));
		ClassSession session = sessions.findByIdForUpdate(sessionId).orElseThrow(() -> RecurringScheduleException.notFound("session"));
		if (resourceLocks != null && (!session.getTeachingCoach().getId().equals(currentCoachLockId) || !java.util.Objects.equals(session.getRoom() == null ? null : session.getRoom().getId(), currentRoomLockId)))
			throw RecurringScheduleException.sessionConflict();
		if (session.getStatus() != ClassSessionStatus.SCHEDULED)
			throw RecurringScheduleException.sessionNotScheduled();
		if (!request.expectedVersion().equals(session.getVersion()))
			throw RecurringScheduleException.versionConflict();
		if (clock.instant().isAfter(session.getStartTime().minus(Duration.ofHours(24)))) {
			throw RecurringScheduleException.rescheduleWindowClosed();
		}
		UUID coachId = request.coachId() == null ? session.getTeachingCoach().getId() : request.coachId();
		UUID currentRoomId = session.getRoom() == null ? null : session.getRoom().getId();
		UUID roomId = request.roomId() == null ? currentRoomId : request.roomId();
		Resources resources = validateResources(session.getSportClass().getId(), coachId, roomId, session.getCapacity());
		ensureAvailable(resources, request.startTime(), request.endTime(), session.getCapacity(), sessionId);
		if (!sessions.findBookedMemberConflictIds(sessionId, request.startTime(), request.endTime()).isEmpty()) {
			throw RecurringScheduleException.memberTimeConflict();
		}
		Map<String, Object> before = scheduleSnapshot(session);
		session.updateAssignment(resources.coach(), resources.room());
		session.updateSchedule(request.startTime(), request.endTime());
		try {
			sessions.saveAndFlush(session);
		} catch (DataIntegrityViolationException exception) {
			throw RecurringScheduleException.sessionConflict();
		}
		audits.save(new AuditEvent(UUID.randomUUID(), managerId, "SESSION_RESCHEDULED", "CLASS_SESSION", sessionId, reason, before, scheduleSnapshot(session)));
		if (notifications != null) {
			for (UUID memberId : sessions.findBookedMemberIds(sessionId)) {
				notifications.write("session-rescheduled:" + sessionId + ":" + session.getVersion(), memberId, "SESSION_RESCHEDULED", "CLASS_SESSION", sessionId, Map.of("startTime", request.startTime().toString(), "endTime", request.endTime().toString()));
			}
		}
		return ClassSessionDetailResponse.from(session);
	}

	private Map<String, Object> scheduleSnapshot(ClassSession session) {
		Map<String, Object> result = new java.util.HashMap<>();
		result.put("startTime", session.getStartTime().toString());
		result.put("endTime", session.getEndTime().toString());
		result.put("coachId", session.getTeachingCoach().getId().toString());
		result.put("roomId", session.getRoom() == null ? null : session.getRoom().getId().toString());
		return result;
	}

	public void moveForRoomImpact(UUID managerId, ClassSession session, Room targetRoom, String reason) {
		ensureActiveManager(managerId);
		if (session.getStatus() != ClassSessionStatus.SCHEDULED && session.getStatus() != ClassSessionStatus.IN_PROGRESS) {
			throw RecurringScheduleException.sessionNotScheduled();
		}
		if (targetRoom.getStatus() != RoomStatus.ACTIVE)
			throw RecurringScheduleException.inactive("room");
		if (session.getCapacity() > targetRoom.getCapacity())
			throw RecurringScheduleException.capacityConflict();
		ensureAvailable(new Resources(session.getSportClass(), session.getTeachingCoach(), targetRoom), session.getStartTime(), session.getEndTime(), session.getCapacity(), session.getId());
		UUID oldRoomId = session.getRoom().getId();
		session.updateAssignment(session.getTeachingCoach(), targetRoom);
		try {
			sessions.saveAndFlush(session);
		} catch (DataIntegrityViolationException exception) {
			throw RecurringScheduleException.sessionConflict();
		}
		audits.save(new AuditEvent(UUID.randomUUID(), managerId, "SESSION_ASSIGNMENT_CHANGED", "CLASS_SESSION", session.getId(), reason, Map.of("roomId", oldRoomId.toString()), Map.of("roomId", targetRoom.getId().toString())));
	}

	public void rescheduleForRoomImpact(UUID managerId, ClassSession session, Instant startTime, Instant endTime, String reason) {
		ensureActiveManager(managerId);
		if (session.getStatus() != ClassSessionStatus.SCHEDULED) {
			throw RecurringScheduleException.sessionNotScheduled();
		}
		validateTimes(startTime, endTime);
		ensureAvailable(new Resources(session.getSportClass(), session.getTeachingCoach(), session.getRoom()), startTime, endTime, session.getCapacity(), session.getId());
		Instant oldStart = session.getStartTime();
		Instant oldEnd = session.getEndTime();
		session.updateSchedule(startTime, endTime);
		try {
			sessions.saveAndFlush(session);
		} catch (DataIntegrityViolationException exception) {
			throw RecurringScheduleException.sessionConflict();
		}
		audits.save(new AuditEvent(UUID.randomUUID(), managerId, "SESSION_RESCHEDULED", "CLASS_SESSION", session.getId(), reason, Map.of("startTime", oldStart.toString(), "endTime", oldEnd.toString()), Map.of("startTime", startTime.toString(), "endTime", endTime.toString())));
	}

	ClassSession newScheduled(Resources resources, RecurringSchedule recurringSchedule, Instant startTime, Instant endTime, int capacity, UUID managerId) {
		return new ClassSession(UUID.randomUUID(), resources.sportClass(), recurringSchedule, resources.coach(), resources.room(), startTime, endTime, capacity, managerId);
	}

	record Resources(SportClass sportClass, Account coach, Room room) {
	}

	private final ClassSessionCatalog classSessionCatalog;

	private final ClassSessionResources classSessionResources;
}
