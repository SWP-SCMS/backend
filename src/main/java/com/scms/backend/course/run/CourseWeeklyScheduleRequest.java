package com.scms.backend.course.run;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public record CourseWeeklyScheduleRequest(@NotNull LocalDate openingDate, @NotEmpty @Size(max = 7) List<@NotNull @Min(1) @Max(7) Integer> weekdays, @NotNull LocalTime startTime, @NotNull LocalTime endTime, @Min(1) @Max(366) int totalUnits) {
}
