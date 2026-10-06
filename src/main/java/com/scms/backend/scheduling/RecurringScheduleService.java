package com.scms.backend.scheduling;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecurringScheduleService {

	private static final ZoneId GYM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
	private static final int OCCURRENCE_COUNT = 30;

	private final RecurringScheduleRepository schedules;
	private final ClassSessionRepository sessions;
	private final ClassSessionService classSessionService;
	private final Clock clock;

	RecurringScheduleService(RecurringScheduleRepository schedules, ClassSessionRepository sessions,
			ClassSessionService classSessionService, Clock clock) {
		this.schedules = schedules;
		this.sessions = sessions;
		this.classSessionService = classSessionService;
		this.clock = clock;
	}

	@Transactional
	RecurringScheduleResponse create(UUID managerId, RecurringScheduleCreateRequest request) {
		classSessionService.ensureActiveManager(managerId);
		validateRequest(request);
		ClassSessionService.Resources resources = classSessionService.validateResources(request.classId(),
			request.coachId(), request.roomId(), request.capacity());

		List<Occurrence> occurrences = occurrences(request);
		if (!occurrences.getFirst().start().isAfter(clock.instant())) {
			throw RecurringScheduleException.validation("startTime", "the first Session must be in the future");
		}
		for (Occurrence occurrence : occurrences) {
			classSessionService.ensureAvailable(resources, occurrence.start(), occurrence.end());
		}

		UUID scheduleId = UUID.randomUUID();
		RecurringSchedule schedule = new RecurringSchedule(scheduleId, resources.sportClass(), resources.coach(),
			resources.room(), request.startDate(),
			request.weekdays().stream().map(Integer::shortValue).toArray(Short[]::new), request.startTime(),
			request.endTime(), request.capacity(), managerId);
		List<ClassSession> newSessions = occurrences.stream()
			.map(occurrence -> classSessionService.newScheduled(resources, schedule, occurrence.start(),
				occurrence.end(), request.capacity(), managerId))
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

	private record Occurrence(Instant start, Instant end) { }
}
