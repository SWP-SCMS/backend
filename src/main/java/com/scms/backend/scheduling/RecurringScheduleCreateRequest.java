package com.scms.backend.scheduling;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record RecurringScheduleCreateRequest(UUID classId, UUID coachId, UUID roomId,
		LocalDate startDate, List<Integer> weekdays, LocalTime startTime, LocalTime endTime,
		Integer capacity) {
}
