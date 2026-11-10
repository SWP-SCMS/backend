package com.scms.backend.course.enrollment;

import static org.assertj.core.api.Assertions.assertThat;
import com.scms.backend.course.CourseAttendanceStatus;
import org.junit.jupiter.api.Test;

class CourseEnrollmentModelsTests {

	@Test
	void onlyPresentAndAbsentAreWritableAttendanceStates() {
		assertThat(CourseAttendanceStatus.writableValues()).containsExactly(CourseAttendanceStatus.PRESENT, CourseAttendanceStatus.ABSENT);
	}
}
