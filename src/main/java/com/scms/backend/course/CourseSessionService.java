package com.scms.backend.course;

import java.math.BigInteger;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.scheduling.CoachLeaveRepository;
import com.scms.backend.scheduling.Room;
import com.scms.backend.scheduling.RoomRepository;
import com.scms.backend.scheduling.RoomStatus;
import com.scms.backend.scheduling.SharedResourceLockService;
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
	private final com.scms.backend.scheduling.ClassSessionRepository gymSessions;
	private final SharedResourceLockService resourceLocks;

	CourseSessionService(CourseRunRepository runs, CourseSessionRepository sessions, AccountRepository accounts,
			RoomRepository rooms, CoachLeaveRepository leaves,
			com.scms.backend.scheduling.ClassSessionRepository gymSessions, SharedResourceLockService resourceLocks) {
		this.runs = runs;
		this.sessions = sessions;
		this.accounts = accounts;
		this.rooms = rooms;
		this.leaves = leaves;
		this.gymSessions = gymSessions;
		this.resourceLocks = resourceLocks;
	}

	@Transactional
	public CourseSession create(UUID runId, int unitNumber, UUID coachId, UUID roomId, Instant startAt,
			Instant endAt, BigInteger allocationVnd) {
		if (runId == null || unitNumber <= 0 || coachId == null || roomId == null || startAt == null
				|| endAt == null || !startAt.isBefore(endAt) || allocationVnd == null || allocationVnd.signum() <= 0) {
			throw CourseException.invalidSession();
		}
		CourseRun run = runs.findById(runId).orElseThrow(CourseException::invalidSession);
		resourceLocks.lock(List.of(coachId), List.of(roomId));
		Account coach = accounts.findById(coachId).orElseThrow(CourseException::resourceConflict);
		Room room = rooms.findById(roomId).orElseThrow(CourseException::resourceConflict);
		validateResources(run, coach, room, startAt, endAt, null);
		CourseSession session = new CourseSession(UUID.randomUUID(), run, unitNumber, room, coach, startAt,
			endAt, allocationVnd);
		try {
			return sessions.saveAndFlush(session);
		}
		catch (DataIntegrityViolationException exception) {
			throw CourseException.invalidSession();
		}
	}

	@Transactional
	public CourseSession reschedule(UUID sessionId, UUID roomId, UUID coachId, Instant startAt, Instant endAt) {
		CourseSession session = sessions.findByIdForUpdate(sessionId).orElseThrow(CourseException::invalidSession);
		UUID requestedRoomId = roomId == null ? session.getRoom().getId() : roomId;
		UUID requestedCoachId = coachId == null ? session.getCoach().getId() : coachId;
		resourceLocks.lock(List.of(requestedCoachId), List.of(requestedRoomId));
		Room room = rooms.findById(requestedRoomId).orElseThrow(CourseException::resourceConflict);
		Account coach = accounts.findById(requestedCoachId).orElseThrow(CourseException::resourceConflict);
		validateResources(session.getCourseRun(), coach, room, startAt, endAt, sessionId);
		if (session.getStatus() != CourseSessionStatus.SCHEDULED
				&& session.getStatus() != CourseSessionStatus.MAKEUP_SCHEDULED) {
			throw CourseException.invalidSession();
		}
		session.reschedule(startAt, endAt);
		session.updateAssignment(room, coach);
		return sessions.saveAndFlush(session);
	}

	private void validateResources(CourseRun run, Account coach, Room room, Instant startAt, Instant endAt,
			UUID excludeSessionId) {
		if (startAt == null || endAt == null || !startAt.isBefore(endAt)) throw CourseException.invalidSession();
		if (coach.getRole() != AccountRole.COACH || coach.getStatus() != AccountStatus.ACTIVE
				|| room.getStatus() != RoomStatus.ACTIVE) throw CourseException.resourceConflict();
		if (run.getCapacity() > room.getCapacity()) throw CourseException.roomCapacityConflict();
		boolean overlap = excludeSessionId == null
			? sessions.existsResourceOverlap(coach.getId(), room.getId(), startAt, endAt)
			: sessions.existsResourceOverlapExcluding(excludeSessionId, coach.getId(), room.getId(), startAt, endAt);
		if (overlap || gymSessions.existsOverlap(coach.getId(), room.getId(), startAt, endAt)
				|| leaves.existsApprovedOverlap(coach.getId(), startAt, endAt)
				|| rooms.existsSuspensionOverlap(room.getId(), startAt, endAt)) {
			throw CourseException.resourceConflict();
		}
	}

	@Transactional(readOnly = true)
	public List<UUID> roomImpactIds(UUID roomId, Instant startAt, Instant endAt, boolean permanentDisable, Instant now) {
		return sessions.findRoomImpactIds(roomId, startAt, endAt, permanentDisable, now);
	}
}
