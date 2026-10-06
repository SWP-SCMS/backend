package com.scms.backend.scheduling;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.auth.InvalidAuthenticatedAccountException;
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
	private final Clock clock;

	ClassSessionService(ClassSessionRepository sessions, SportClassRepository classes, RoomRepository rooms,
			AccountRepository accounts, Clock clock) {
		this.sessions = sessions;
		this.classes = classes;
		this.rooms = rooms;
		this.accounts = accounts;
		this.clock = clock;
	}

	@Transactional
	ClassSessionResponse create(UUID managerId, ClassSessionCreateRequest request) {
		ensureActiveManager(managerId);
		if (request == null) throw RecurringScheduleException.validation("request", "must be a JSON object");
		validateTimes(request.startTime(), request.endTime());
		Resources resources = validateResources(request.classId(), request.coachId(), request.roomId(),
			request.capacity());
		ensureAvailable(resources, request.startTime(), request.endTime());
		ClassSession session = newScheduled(resources, null, request.startTime(), request.endTime(),
			request.capacity(), managerId);
		try {
			sessions.saveAndFlush(session);
		}
		catch (DataIntegrityViolationException exception) {
			throw RecurringScheduleException.sessionConflict();
		}
		return ClassSessionResponse.from(session);
	}

	@Transactional
	ClassSessionPageResponse list(UUID managerId, Instant from, Instant to, UUID classId, UUID coachId,
			UUID roomId, ClassSessionStatus status, Pageable pageable) {
		ensureActiveManager(managerId);
		if (from != null && to != null && !to.isAfter(from)) {
			throw RecurringScheduleException.validation("to", "must be later than from");
		}
		advanceStatuses();

		Specification<ClassSession> filters = (root, ignored, builder) -> builder.conjunction();
		if (from != null) filters = filters.and((root, ignored, builder) ->
			builder.greaterThan(root.get("endTime"), from));
		if (to != null) filters = filters.and((root, ignored, builder) ->
			builder.lessThan(root.get("startTime"), to));
		if (classId != null) filters = filters.and((root, ignored, builder) ->
			builder.equal(root.get("sportClass").get("id"), classId));
		if (coachId != null) filters = filters.and((root, ignored, builder) ->
			builder.equal(root.get("teachingCoach").get("id"), coachId));
		if (roomId != null) filters = filters.and((root, ignored, builder) ->
			builder.equal(root.get("room").get("id"), roomId));
		if (status != null) filters = filters.and((root, ignored, builder) ->
			builder.equal(root.get("status"), status));

		var page = sessions.findAll(filters, pageable);
		return new ClassSessionPageResponse(page.getContent().stream().map(ClassSessionResponse::from).toList(),
			page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
	}

	@Transactional
	ClassSessionDetailResponse get(UUID managerId, UUID sessionId) {
		ensureActiveManager(managerId);
		advanceStatuses();
		ClassSession session = sessions.findById(sessionId)
			.orElseThrow(() -> RecurringScheduleException.notFound("session"));
		return ClassSessionDetailResponse.from(session);
	}

	private void advanceStatuses() {
		Instant now = clock.instant();
		sessions.advanceScheduledToInProgress(now);
		sessions.advanceInProgressToCompleted(now);
	}

	void ensureActiveManager(UUID managerId) {
		if (!accounts.existsByIdAndRoleAndStatus(managerId, AccountRole.MANAGER, AccountStatus.ACTIVE)) {
			throw new InvalidAuthenticatedAccountException();
		}
	}

	void validateTimes(Instant startTime, Instant endTime) {
		if (startTime == null || !startTime.isAfter(clock.instant())) {
			throw RecurringScheduleException.validation("startTime", "must be in the future");
		}
		if (endTime == null || !endTime.isAfter(startTime)) {
			throw RecurringScheduleException.validation("endTime", "must be later than startTime");
		}
	}

	Resources validateResources(UUID classId, UUID coachId, UUID roomId, Integer capacity) {
		if (classId == null) throw RecurringScheduleException.validation("classId", "is required");
		if (coachId == null) throw RecurringScheduleException.validation("coachId", "is required");
		if (roomId == null) throw RecurringScheduleException.validation("roomId", "is required");
		if (capacity == null || capacity <= 0) {
			throw RecurringScheduleException.validation("capacity", "must be greater than zero");
		}
		SportClass sportClass = classes.findById(classId)
			.orElseThrow(() -> RecurringScheduleException.notFound("class"));
		if (sportClass.getStatus() != SportClassStatus.ACTIVE
				|| sportClass.getDiscipline().getStatus() != DisciplineStatus.ACTIVE) {
			throw RecurringScheduleException.inactive("class");
		}
		Account coach = accounts.findById(coachId)
			.orElseThrow(() -> RecurringScheduleException.notFound("coach"));
		if (coach.getRole() != AccountRole.COACH || coach.getStatus() != AccountStatus.ACTIVE) {
			throw RecurringScheduleException.coachInvalid();
		}
		Room room = rooms.findById(roomId).orElseThrow(() -> RecurringScheduleException.notFound("room"));
		if (room.getStatus() != RoomStatus.ACTIVE) throw RecurringScheduleException.inactive("room");
		if (capacity > room.getCapacity()) throw RecurringScheduleException.capacityConflict();
		return new Resources(sportClass, coach, room);
	}

	void ensureAvailable(Resources resources, Instant startTime, Instant endTime) {
		if (sessions.existsOverlap(resources.coach().getId(), resources.room().getId(), startTime, endTime)) {
			throw RecurringScheduleException.sessionConflict();
		}
	}

	ClassSession newScheduled(Resources resources, RecurringSchedule recurringSchedule, Instant startTime,
			Instant endTime, int capacity, UUID managerId) {
		return new ClassSession(UUID.randomUUID(), resources.sportClass(), recurringSchedule, resources.coach(),
			resources.room(), startTime, endTime, capacity, managerId);
	}

	record Resources(SportClass sportClass, Account coach, Room room) { }
}
