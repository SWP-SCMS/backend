package com.scms.backend.course.session;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.course.CourseException;
import com.scms.backend.course.CourseRun;
import com.scms.backend.course.CourseRunStatus;
import com.scms.backend.course.CourseSession;
import com.scms.backend.course.CourseSessionStatus;
import com.scms.backend.course.run.CourseRunRepository;
import com.scms.backend.course.schedulechange.CourseScheduleChangeService;
import com.scms.backend.scheduling.Room;
import com.scms.backend.scheduling.RoomStatus;
import com.scms.backend.scheduling.coachleave.CoachLeaveRepository;
import com.scms.backend.scheduling.resource.SharedResourceLockService;
import com.scms.backend.scheduling.room.RoomRepository;
import java.math.BigInteger;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CourseSessionService {

	private final CourseRunRepository runs;

	private final CourseSessionRepository sessions;

	private final AccountRepository accounts;

	private final RoomRepository rooms;

	private final CoachLeaveRepository leaves;

	private final com.scms.backend.scheduling.session.ClassSessionRepository gymSessions;

	private final SharedResourceLockService resourceLocks;

	private final CourseScheduleChangeService scheduleChanges;

	CourseSessionService(CourseRunRepository runs, CourseSessionRepository sessions, AccountRepository accounts, RoomRepository rooms, CoachLeaveRepository leaves, com.scms.backend.scheduling.session.ClassSessionRepository gymSessions, SharedResourceLockService resourceLocks, CourseScheduleChangeService scheduleChanges) {
		this.runs = runs;
		this.sessions = sessions;
		this.accounts = accounts;
		this.rooms = rooms;
		this.leaves = leaves;
		this.gymSessions = gymSessions;
		this.resourceLocks = resourceLocks;
		this.scheduleChanges = scheduleChanges;
	}

	@Transactional
	public CourseSession create(UUID runId, int unitNumber, UUID coachId, UUID roomId, Instant startAt, Instant endAt, BigInteger allocationVnd) {
		return create(runId, unitNumber, coachId, roomId, startAt, endAt, allocationVnd, false);
	}

	CourseSession createMakeup(UUID runId, int unitNumber, UUID coachId, UUID roomId, Instant startAt, Instant endAt, BigInteger allocationVnd) {
		return create(runId, unitNumber, coachId, roomId, startAt, endAt, allocationVnd, true);
	}

	private CourseSession create(UUID runId, int unitNumber, UUID coachId, UUID roomId, Instant startAt, Instant endAt, BigInteger allocationVnd, boolean makeup) {
		if (runId == null || unitNumber <= 0 || coachId == null || roomId == null || startAt == null || endAt == null || !startAt.isBefore(endAt) || allocationVnd == null || allocationVnd.signum() <= 0 || allocationVnd.toString().length() > 19) {
			throw CourseException.invalidSession();
		}
		resourceLocks.lock(List.of(coachId), List.of(roomId));
		CourseRun run = runs.findByIdForUpdate(runId).orElseThrow(CourseException::invalidSession);
		if (unitNumber > run.getTotalUnits())
			throw CourseException.invalidSession();
		Account coach = accounts.findById(coachId).orElseThrow(CourseException::resourceConflict);
		Room room = rooms.findById(roomId).orElseThrow(CourseException::resourceConflict);
		validateResources(run, coach, room, startAt, endAt, null, !makeup);
		CourseSession session = new CourseSession(UUID.randomUUID(), run, unitNumber, room, coach, startAt, endAt, allocationVnd);
		if (makeup)
			session.scheduleMakeup();
		try {
			return sessions.saveAndFlush(session);
		} catch (DataIntegrityViolationException exception) {
			throw CourseException.invalidSession();
		}
	}

	@Transactional
	public CourseSession reschedule(UUID sessionId, UUID roomId, UUID coachId, Instant startAt, Instant endAt) {
		return reschedule(sessionId, roomId, coachId, startAt, endAt, null);
	}

	@Transactional
	public CourseSession reschedule(UUID sessionId, UUID roomId, UUID coachId, Instant startAt, Instant endAt, Long expectedVersion) {
		UUID currentRoomId = sessions.findRoomIdById(sessionId).orElseThrow(CourseException::invalidSession);
		UUID currentCoachId = sessions.findCoachIdById(sessionId).orElseThrow(CourseException::invalidSession);
		UUID requestedRoomId = roomId == null ? currentRoomId : roomId;
		UUID requestedCoachId = coachId == null ? currentCoachId : coachId;
		resourceLocks.lock(List.of(currentCoachId, requestedCoachId), List.of(currentRoomId, requestedRoomId));
		UUID runId = sessions.findCourseRunIdById(sessionId).orElseThrow(CourseException::invalidSession);
		CourseRun run = runs.findByIdForUpdate(runId).orElseThrow(CourseException::invalidSession);
		CourseSession session = sessions.findByIdForUpdate(sessionId).orElseThrow(CourseException::invalidSession);
		if (expectedVersion != null && !expectedVersion.equals(session.getVersion())) {
			throw CourseException.sessionChanged();
		}
		if (!session.getRoom().getId().equals(currentRoomId) || !session.getCoach().getId().equals(currentCoachId)) {
			throw CourseException.resourceConflict();
		}
		Room room = rooms.findById(requestedRoomId).orElseThrow(CourseException::resourceConflict);
		Account coach = accounts.findById(requestedCoachId).orElseThrow(CourseException::resourceConflict);
		validateResources(session.getCourseRun(), coach, room, startAt, endAt, sessionId, false);
		if (session.getStatus() != CourseSessionStatus.SCHEDULED && session.getStatus() != CourseSessionStatus.MAKEUP_SCHEDULED) {
			throw CourseException.invalidSession();
		}
		boolean timeChanged = !startAt.equals(session.getStartAt()) || !endAt.equals(session.getEndAt());
		Instant runStart = startAt, runEnd = endAt;
		if (timeChanged) {
			// Use actual dates, not unit numbers; a moved middle unit can become the first/last.
			for (CourseSession other : sessions.findByCourseRunIdOrderByStartAtAscIdAsc(runId)) {
				if (other.getId().equals(sessionId) || other.getReplacementSession() != null
					|| !List.of(CourseSessionStatus.SCHEDULED, CourseSessionStatus.MAKEUP_SCHEDULED, CourseSessionStatus.COMPLETED).contains(other.getStatus()))
					continue;
				if (other.getStartAt().isBefore(runStart)) runStart = other.getStartAt();
				if (other.getEndAt().isAfter(runEnd)) runEnd = other.getEndAt();
			}
			if (run.getRegistrationClosesAt() != null && runStart.isBefore(run.getRegistrationClosesAt()))
				throw CourseException.validation("startAt", "must not precede registration closing");
		}
		scheduleChanges.notifyReschedule(session, session.getStartAt(), session.getEndAt(), startAt, endAt);
		session.reschedule(startAt, endAt);
		session.updateAssignment(room, coach);
		if (timeChanged && (!runStart.equals(run.getPlannedStartAt()) || !runEnd.equals(run.getPlannedEndAt()))) {
			run.changeSchedule(runStart, runEnd, run.getTotalUnits());
			runs.saveAndFlush(run);
		}
		return sessions.saveAndFlush(session);
	}

	void synchronizeRunWindow(CourseRun run) {
		List<CourseSession> effective = sessions.findByCourseRunIdOrderByStartAtAscIdAsc(run.getId()).stream()
			.filter(session -> session.getReplacementSession() == null)
			.filter(session -> List.of(CourseSessionStatus.SCHEDULED, CourseSessionStatus.MAKEUP_SCHEDULED, CourseSessionStatus.COMPLETED).contains(session.getStatus()))
			.toList();
		if (effective.isEmpty()) return;
		Instant start = effective.stream().map(CourseSession::getStartAt).min(Instant::compareTo).orElseThrow();
		Instant end = effective.stream().map(CourseSession::getEndAt).max(Instant::compareTo).orElseThrow();
		if (run.getRegistrationClosesAt() != null && start.isBefore(run.getRegistrationClosesAt()))
			throw CourseException.validation("startAt", "must not precede registration closing");
		if (!start.equals(run.getPlannedStartAt()) || !end.equals(run.getPlannedEndAt())) {
			run.changeSchedule(start, end, run.getTotalUnits());
			runs.saveAndFlush(run);
		}
	}

	private void validateResources(CourseRun run, Account coach, Room room, Instant startAt, Instant endAt, UUID excludeSessionId, boolean requireRunWindow) {
		if (startAt == null || endAt == null || !startAt.isBefore(endAt))
			throw CourseException.invalidSession();
		if (run.getStatus() == CourseRunStatus.COMPLETED || run.getStatus() == CourseRunStatus.CANCELLED || run.getStatus() == CourseRunStatus.SETTLEMENT_PENDING)
			throw CourseException.invalidSession();
		if (requireRunWindow && run.getPlannedStartAt() != null && (startAt.isBefore(run.getPlannedStartAt()) || endAt.isAfter(run.getPlannedEndAt())))
			throw CourseException.invalidSession();
		validateAssignment(run.getCapacity(), coach, room, startAt, endAt);
		boolean overlap = excludeSessionId == null ? sessions.existsResourceOverlap(coach.getId(), room.getId(), startAt, endAt) : sessions.existsResourceOverlapExcluding(excludeSessionId, coach.getId(), room.getId(), startAt, endAt);
		if (overlap)
			throw CourseException.resourceConflict();
	}

	public void validateAssignment(int capacity, Account coach, Room room, Instant startAt, Instant endAt) {
		if (coach.getRole() != AccountRole.COACH || coach.getStatus() != AccountStatus.ACTIVE || room.getStatus() != RoomStatus.ACTIVE)
			throw CourseException.resourceConflict();
		if (capacity > room.getCapacity())
			throw CourseException.roomCapacityConflict();
		if (gymSessions.existsOverlap(coach.getId(), room.getId(), startAt, endAt) || leaves.existsApprovedOverlap(coach.getId(), startAt, endAt) || rooms.existsSuspensionOverlap(room.getId(), startAt, endAt)) {
			throw CourseException.resourceConflict();
		}
	}

	@Transactional(readOnly = true)
	public List<UUID> roomImpactIds(UUID roomId, Instant startAt, Instant endAt, boolean permanentDisable, Instant now) {
		return sessions.findRoomImpactIds(roomId, startAt, endAt, permanentDisable, now);
	}
}
