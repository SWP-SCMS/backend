package com.scms.backend.course.run;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import com.scms.backend.course.CourseException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class CourseWeeklyScheduleTests {

	private CourseWeeklyScheduleRequest request(LocalDate date, List<Integer> days, int units) {
		return new CourseWeeklyScheduleRequest(date, days, LocalTime.of(18, 0), LocalTime.of(19, 0), units);
	}

	@Test
	void mondayWednesdayFridayComputesLastSessionAcrossMonthAndYear() {
		var result = CourseWeeklySchedule.generate(request(LocalDate.of(2030, 12, 30), List.of(1, 3, 5), 4));
		assertThat(result.plannedStartAt()).isEqualTo(Instant.parse("2030-12-30T11:00:00Z"));
		assertThat(result.plannedEndAt()).isEqualTo(Instant.parse("2031-01-06T12:00:00Z"));
		assertThat(result.sessions()).extracting(CourseWeeklySlot::unitNumber).containsExactly(1, 2, 3, 4);
		assertThat(result.sessions()).extracting(s -> s.startAt().atZone(CourseWeeklySchedule.ZONE).toLocalDate()).containsExactly(LocalDate.of(2030, 12, 30), LocalDate.of(2031, 1, 1), LocalDate.of(2031, 1, 3), LocalDate.of(2031, 1, 6));
	}

	@Test
	void singleSundayAndLeapDayUseVietnamLocalDate() {
		var sunday = CourseWeeklySchedule.generate(request(LocalDate.of(2032, 2, 29), List.of(7), 1));
		assertThat(sunday.plannedEndAt()).isEqualTo(Instant.parse("2032-02-29T12:00:00Z"));
		var early = CourseWeeklySchedule.generate(new CourseWeeklyScheduleRequest(LocalDate.of(2032, 2, 29), List.of(7), LocalTime.of(0, 30), LocalTime.of(1, 30), 2));
		assertThat(early.plannedStartAt()).isEqualTo(Instant.parse("2032-02-28T17:30:00Z"));
		assertThat(early.plannedEndAt()).isEqualTo(Instant.parse("2032-03-06T18:30:00Z"));
	}

	@Test
	void rejectsMissingDuplicateOutOfRangeAndMismatchedDays() {
		for (var days : List.of(List.<Integer>of(), List.of(1, 1), List.of(0), List.of(8), List.of(2))) {
			assertThatThrownBy(() -> CourseWeeklySchedule.generate(request(LocalDate.of(2030, 12, 30), days, 4))).isInstanceOf(CourseException.class);
		}
	}

	@Test
	void rejectsInvalidCountsAndOvernightTimes() {
		for (int count : new int[] { 0, -1, 367 }) assertThatThrownBy(() -> CourseWeeklySchedule.generate(request(LocalDate.of(2030, 12, 30), List.of(1), count))).isInstanceOf(CourseException.class);
		assertThatThrownBy(() -> CourseWeeklySchedule.generate(new CourseWeeklyScheduleRequest(LocalDate.of(2030, 12, 30), List.of(1), LocalTime.of(19, 0), LocalTime.of(18, 0), 1))).isInstanceOf(CourseException.class);
	}
}
