package com.scms.backend.scheduling;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

public record RecurringScheduleCreateRequest(UUID classId, UUID coachId, UUID roomId,
		LocalDate startDate, List<Integer> weekdays, LocalTime startTime, LocalTime endTime,
		Integer capacity,
		@Schema(description = "Number of Sessions to create; defaults to 30 when omitted",
			minimum = "1", maximum = "100", defaultValue = "30") Integer sessionCount) {

	public RecurringScheduleCreateRequest(UUID classId, UUID coachId, UUID roomId, LocalDate startDate,
			List<Integer> weekdays, LocalTime startTime, LocalTime endTime, Integer capacity) {
		this(classId, coachId, roomId, startDate, weekdays, startTime, endTime, capacity, null);
	}
}
