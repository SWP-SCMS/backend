package com.scms.backend.course;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class CourseAttendancePolicyTests {
	private static final Instant START = Instant.parse("2035-01-01T10:00:00Z");
	private static final Instant END = Instant.parse("2035-01-01T11:00:00Z");

	@Test void beforeWindowIsTooEarly() {
		assertThat(decision(START.minusSeconds(1))).extracting(CourseAttendancePolicy.Decision::writable,
			CourseAttendancePolicy.Decision::reason).containsExactly(false, "TOO_EARLY");
	}

	@Test void duringWindowIsWritable() {
		assertThat(decision(START)).extracting(CourseAttendancePolicy.Decision::writable,
			CourseAttendancePolicy.Decision::reason).containsExactly(true, "WRITABLE");
	}

	@Test void exactEndPlusThirtyMinutesIsClosed() {
		assertThat(decision(END.plusSeconds(1800))).extracting(CourseAttendancePolicy.Decision::writable,
			CourseAttendancePolicy.Decision::reason).containsExactly(false, "WINDOW_CLOSED");
	}

	@Test void cancelledSessionWinsOverTiming() {
		assertThat(CourseAttendancePolicy.evaluate("CANCELLED_PENDING_MAKEUP", "IN_PROGRESS", START, END, START, true)
			.reason()).isEqualTo("SESSION_CANCELLED");
	}

	@Test void inactiveRunIsNotWritable() {
		assertThat(CourseAttendancePolicy.evaluate("SCHEDULED", "CANCELLED", START, END, START, true)
			.reason()).isEqualTo("COURSE_RUN_NOT_ACTIVE");
	}

	@Test void automaticCompletionPreservesThirtyMinuteAttendanceWindow() {
		assertThat(CourseAttendancePolicy.evaluate("COMPLETED", "COMPLETED", START, END, END.plusSeconds(1799), true)
			.writable()).isTrue();
		assertThat(CourseAttendancePolicy.evaluate("COMPLETED", "COMPLETED", START, END, END.plusSeconds(1800), true)
			.reason()).isEqualTo("WINDOW_CLOSED");
	}

	@Test void unassignedCoachIsHiddenByPolicy() {
		assertThat(CourseAttendancePolicy.evaluate("SCHEDULED", "IN_PROGRESS", START, END, START, false)
			.reason()).isEqualTo("NOT_ASSIGNED_COACH");
	}

	private CourseAttendancePolicy.Decision decision(Instant now) {
		return CourseAttendancePolicy.evaluate("SCHEDULED", "IN_PROGRESS", START, END, now, true);
	}
}
