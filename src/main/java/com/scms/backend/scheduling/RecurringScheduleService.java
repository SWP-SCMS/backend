package com.scms.backend.scheduling;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.auth.InvalidAuthenticatedAccountException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecurringScheduleService {

	private static final ZoneId GYM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
	private static final int OCCURRENCE_COUNT = 30;

	private final RecurringScheduleRepository schedules;
	private final ClassSessionRepository sessions;
	private final SportClassRepository classes;
	private final RoomRepository rooms;
	private final AccountRepository accounts;
	private final Clock clock;

	RecurringScheduleService(RecurringScheduleRepository schedules, ClassSessionRepository sessions,
			SportClassRepository classes, RoomRepository rooms, AccountRepository accounts, Clock clock) {
		this.schedules = schedules;
		this.sessions = sessions;
		this.classes = classes;
		this.rooms = rooms;
		this.accounts = accounts;
		this.clock = clock;
	}

	@Transactional
	RecurringScheduleResponse create(UUID managerId, RecurringScheduleCreateRequest request) {
		ensureActiveManager(managerId);
		validateRequest(request);
		SportClass sportClass = classes.findById(request.classId())
			.orElseThrow(() -> RecurringScheduleException.notFound("class"));
		if (sportClass.getStatus() != SportClassStatus.ACTIVE
				|| sportClass.getDiscipline().getStatus() != DisciplineStatus.ACTIVE) {
			throw RecurringScheduleException.inactive("class");
		}
		Account coach = accounts.findById(request.coachId())
			.orElseThrow(() -> RecurringScheduleException.notFound("coach"));
		if (coach.getRole() != AccountRole.COACH || coach.getStatus() != AccountStatus.ACTIVE) {
			throw RecurringScheduleException.coachInvalid();
		}
		Room room = rooms.findById(request.roomId())
			.orElseThrow(() -> RecurringScheduleException.notFound("room"));
		if (room.getStatus() != RoomStatus.ACTIVE) throw RecurringScheduleException.inactive("room");
		if (request.capacity() > room.getCapacity()) throw RecurringScheduleException.capacityConflict();

		List<Occurrence> occurrences = occurrences(request);
		if (!occurrences.getFirst().start().isAfter(clock.instant())) {
			throw RecurringScheduleException.validation("startTime", "the first Session must be in the future");
		}
		for (Occurrence occurrence : occurrences) {
			if (sessions.existsOverlap(coach.getId(), room.getId(), occurrence.start(), occurrence.end())) {
				throw RecurringScheduleException.sessionConflict();
			}
		}

		UUID scheduleId = UUID.randomUUID();
		RecurringSchedule schedule = new RecurringSchedule(scheduleId, sportClass, coach, room, request.startDate(),
			request.weekdays().stream().map(Integer::shortValue).toArray(Short[]::new), request.startTime(),
			request.endTime(), request.capacity(), managerId);
		List<ClassSession> newSessions = occurrences.stream()
			.map(occurrence -> new ClassSession(UUID.randomUUID(), sportClass, schedule, coach, room,
				occurrence.start(), occurrence.end(), request.capacity(), managerId))
			.toList();
		try {
			schedules.saveAndFlush(schedule);
			sessions.saveAllAndFlush(newSessions);
		}
		catch (DataIntegrityViolationException exception) {
			throw RecurringScheduleException.scheduleConflict();
		}
		return new RecurringScheduleResponse(scheduleId, OCCURRENCE_COUNT,
			newSessions.stream().map(ClassSession::getId).toList());
	}

	private List<Occurrence> occurrences(RecurringScheduleCreateRequest request) {
		List<Occurrence> occurrences = new ArrayList<>(OCCURRENCE_COUNT);
		LocalDate date = request.startDate();
		while (occurrences.size() < OCCURRENCE_COUNT) {
			if (request.weekdays().contains(date.getDayOfWeek().getValue())) {
				Instant start = LocalDateTime.of(date, request.startTime()).atZone(GYM_ZONE).toInstant();
				Instant end = LocalDateTime.of(date, request.endTime()).atZone(GYM_ZONE).toInstant();
				occurrences.add(new Occurrence(start, end));
			}
			date = date.plusDays(1);
		}
		return occurrences;
	}

	private void validateRequest(RecurringScheduleCreateRequest request) {
		if (request == null) throw RecurringScheduleException.validation("request", "must be a JSON object");
		if (request.classId() == null) throw RecurringScheduleException.validation("classId", "is required");
		if (request.coachId() == null) throw RecurringScheduleException.validation("coachId", "is required");
		if (request.roomId() == null) throw RecurringScheduleException.validation("roomId", "is required");
		if (request.startDate() == null || request.startDate().isBefore(LocalDate.now(clock.withZone(GYM_ZONE)))) {
			throw RecurringScheduleException.validation("startDate", "must be today or a future date");
		}
		if (request.weekdays() == null || request.weekdays().isEmpty()
				|| request.weekdays().stream().anyMatch(day -> day == null || day < 1 || day > 7)
				|| request.weekdays().stream().distinct().count() != request.weekdays().size()) {
			throw RecurringScheduleException.validation("weekdays", "must contain unique ISO weekdays from 1 to 7");
		}
		if (request.startTime() == null) throw RecurringScheduleException.validation("startTime", "is required");
		if (request.endTime() == null || !request.endTime().isAfter(request.startTime())) {
			throw RecurringScheduleException.validation("endTime", "must be later than startTime");
		}
		if (request.capacity() == null || request.capacity() <= 0) {
			throw RecurringScheduleException.validation("capacity", "must be greater than zero");
		}
	}

	private void ensureActiveManager(UUID managerId) {
		if (!accounts.existsByIdAndRoleAndStatus(managerId, AccountRole.MANAGER, AccountStatus.ACTIVE)) {
			throw new InvalidAuthenticatedAccountException();
		}
	}

	private record Occurrence(Instant start, Instant end) { }
}
