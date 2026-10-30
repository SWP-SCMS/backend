package com.scms.backend.course;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CourseEnrollmentModelsTests {
	@Test
	void onlyPresentAndAbsentAreWritableAttendanceStates() {
		assertThat(CourseAttendanceStatus.writableValues())
			.containsExactly(CourseAttendanceStatus.PRESENT, CourseAttendanceStatus.ABSENT);
	}
}
