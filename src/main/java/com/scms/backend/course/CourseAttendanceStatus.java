package com.scms.backend.course;

import java.util.List;

public enum CourseAttendanceStatus {
	NOT_RECORDED,
	PRESENT,
	ABSENT;

	public static List<CourseAttendanceStatus> writableValues() {
		return List.of(PRESENT, ABSENT);
	}
}
