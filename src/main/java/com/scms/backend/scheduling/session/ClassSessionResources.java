package com.scms.backend.scheduling.session;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.course.session.CourseSessionRepository;
import com.scms.backend.scheduling.DisciplineStatus;
import com.scms.backend.scheduling.RecurringScheduleException;
import com.scms.backend.scheduling.Room;
import com.scms.backend.scheduling.RoomStatus;
import com.scms.backend.scheduling.RoomUsageMode;
import com.scms.backend.scheduling.SportClass;
import com.scms.backend.scheduling.SportClassStatus;
import com.scms.backend.scheduling.SportClassType;
import com.scms.backend.scheduling.coachleave.CoachLeaveRepository;
import com.scms.backend.scheduling.resource.SharedResourceLockService;
import com.scms.backend.scheduling.room.RoomRepository;
import com.scms.backend.scheduling.session.ClassSessionService.Resources;
import com.scms.backend.scheduling.sportclass.SportClassRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

final class ClassSessionResources {

	private final ClassSessionRepository sessions;

	private final SportClassRepository classes;

	private final RoomRepository rooms;

	private final AccountRepository accounts;

	private final Clock clock;

	private final CoachLeaveRepository coachLeaves;

	private final CourseSessionRepository courseSessions;

	private final SharedResourceLockService resourceLocks;

	ClassSessionResources(ClassSessionRepository sessions, SportClassRepository classes, RoomRepository rooms, AccountRepository accounts, Clock clock, CoachLeaveRepository coachLeaves, CourseSessionRepository courseSessions, SharedResourceLockService resourceLocks) {
		this.sessions = sessions;
		this.classes = classes;
		this.rooms = rooms;
		this.accounts = accounts;
		this.clock = clock;
		this.coachLeaves = coachLeaves;
		this.courseSessions = courseSessions;
		this.resourceLocks = resourceLocks;
	}

	void validateTimes(Instant startTime, Instant endTime) {
		if (startTime == null || !startTime.isAfter(clock.instant())) {
			throw RecurringScheduleException.validation("startTime", "must be in the future");
		}
		if (endTime == null || !endTime.isAfter(startTime)) {
			throw RecurringScheduleException.validation("endTime", "must be later than startTime");
		}
		if (Duration.between(startTime, endTime).compareTo(Duration.ofMinutes(30)) < 0) {
			throw RecurringScheduleException.validation("endTime", "a Session cannot be shorter than 30 minutes");
		}
		if (Duration.between(startTime, endTime).compareTo(Duration.ofMinutes(90)) > 0) {
			throw RecurringScheduleException.validation("endTime", "a Session cannot be longer than 90 minutes");
		}
	}

	Resources validateResources(UUID classId, UUID coachId, UUID roomId, Integer capacity) {
		if (classId == null)
			throw RecurringScheduleException.validation("classId", "is required");
		if (coachId == null)
			throw RecurringScheduleException.validation("coachId", "is required");
		if (capacity == null || capacity <= 0) {
			throw RecurringScheduleException.validation("capacity", "must be greater than zero");
		}
		if (resourceLocks != null)
			resourceLocks.lock(List.of(coachId), roomId == null ? List.of() : List.of(roomId));
		Optional<SportClass> classResult = classes.findByIdForUpdate(classId);
		if (classResult == null || classResult.isEmpty())
			classResult = classes.findById(classId);
		SportClass sportClass = classResult.orElseThrow(() -> RecurringScheduleException.notFound("class"));
		if (sportClass.getStatus() != SportClassStatus.ACTIVE || sportClass.getDiscipline().getStatus() != DisciplineStatus.ACTIVE) {
			throw RecurringScheduleException.inactive("class");
		}
		if (sportClass.getClassType() == SportClassType.YOGA || "yoga".equalsIgnoreCase(sportClass.getDiscipline().getName().trim())) {
			throw RecurringScheduleException.legacyYoga();
		}
		boolean pt = sportClass.getClassType() == SportClassType.PT_1_1;
		if (pt && capacity != 1)
			throw RecurringScheduleException.ptCapacity();
		if (!pt && roomId == null)
			throw RecurringScheduleException.validation("roomId", "is required");
		Optional<Account> coachResult = accounts.findByIdForUpdate(coachId);
		if (coachResult == null || coachResult.isEmpty())
			coachResult = accounts.findById(coachId);
		Account coach = coachResult.orElseThrow(() -> RecurringScheduleException.notFound("coach"));
		if (coach.getRole() != AccountRole.COACH || coach.getStatus() != AccountStatus.ACTIVE) {
			throw RecurringScheduleException.coachInvalid();
		}
		Room room = null;
		if (roomId != null) {
			Optional<Room> roomResult = rooms.findByIdForUpdate(roomId);
			if (roomResult == null || roomResult.isEmpty())
				roomResult = rooms.findById(roomId);
			room = roomResult.orElseThrow(() -> RecurringScheduleException.notFound("room"));
			if (room.getStatus() != RoomStatus.ACTIVE)
				throw RecurringScheduleException.inactive("room");
			if (capacity > room.getCapacity())
				throw RecurringScheduleException.capacityConflict();
		}
		return new Resources(sportClass, coach, room);
	}

	void ensureAvailable(Resources resources, Instant startTime, Instant endTime, int requestedCapacity, UUID excludeSessionId) {
		UUID roomId = resources.room() == null ? null : resources.room().getId();
		if (courseSessions != null && courseSessions.existsResourceOverlap(resources.coach().getId(), roomId, startTime, endTime))
			throw RecurringScheduleException.sessionConflict();
		boolean coachConflict = excludeSessionId == null ? sessions.existsCoachOverlap(resources.coach().getId(), startTime, endTime) : sessions.existsCoachOverlapExcluding(excludeSessionId, resources.coach().getId(), startTime, endTime);
		if (coachConflict) {
			throw RecurringScheduleException.sessionConflict();
		}
		ensureCoachAvailable(resources.coach().getId(), startTime, endTime);
		if (roomId == null)
			return;
		if (rooms.existsSuspensionOverlap(roomId, startTime, endTime)) {
			throw RecurringScheduleException.sessionConflict();
		}
		boolean roomConflict = excludeSessionId == null ? sessions.existsRoomOverlap(roomId, startTime, endTime) : sessions.existsRoomOverlapExcluding(excludeSessionId, roomId, startTime, endTime);
		if (resources.room().getUsageMode() == RoomUsageMode.EXCLUSIVE) {
			if (roomConflict)
				throw RecurringScheduleException.roomTimeConflict();
			return;
		}
		long occupiedCapacity = excludeSessionId == null ? sessions.sumOverlappingCapacity(roomId, startTime, endTime) : sessions.sumOverlappingCapacityExcluding(excludeSessionId, roomId, startTime, endTime);
		if (occupiedCapacity + requestedCapacity > resources.room().getCapacity()) {
			throw RecurringScheduleException.sharedRoomCapacityExceeded(resources.room().getCapacity(), occupiedCapacity, requestedCapacity);
		}
	}

	void ensureCoachAvailable(UUID coachId, Instant startTime, Instant endTime) {
		if (coachLeaves != null && coachLeaves.existsApprovedOverlap(coachId, startTime, endTime)) {
			throw RecurringScheduleException.coachOnLeave();
		}
	}
}
