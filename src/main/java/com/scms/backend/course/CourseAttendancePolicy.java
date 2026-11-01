package com.scms.backend.course;

import java.time.Instant;
import java.util.Set;

final class CourseAttendancePolicy {
	private static final Set<String> ACTIVE_RUN_STATUSES = Set.of("CONFIRMED", "IN_PROGRESS");

	private CourseAttendancePolicy() {}

	static Decision evaluate(String sessionStatus, String runStatus, Instant startsAt, Instant endsAt,
			Instant now, boolean assignedCoach) {
		if (!assignedCoach) return new Decision(false, "NOT_ASSIGNED_COACH");
		if (!"SCHEDULED".equals(sessionStatus) && !"MAKEUP_SCHEDULED".equals(sessionStatus))
			return new Decision(false, "SESSION_CANCELLED");
		if (runStatus != null && !ACTIVE_RUN_STATUSES.contains(runStatus))
			return new Decision(false, "COURSE_RUN_NOT_ACTIVE");
		if (now.isBefore(startsAt)) return new Decision(false, "TOO_EARLY");
		if (!now.isBefore(endsAt.plusSeconds(1800))) return new Decision(false, "WINDOW_CLOSED");
		return new Decision(true, "WRITABLE");
	}

	record Decision(boolean writable, String reason) {}
}
