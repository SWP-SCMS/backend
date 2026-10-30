package com.scms.backend.course;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

public final class CourseEnrollmentApiModels {
	private CourseEnrollmentApiModels() {}

	@Schema(additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
	public record MemberEnrollment(UUID enrollmentId, UUID courseRunId, String courseCode, String courseName,
			String runCode, Instant enrolledAt, List<MemberSession> sessions) {}

	@Schema(additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
	public record MemberSession(UUID enrollmentSessionId, UUID courseSessionId, int unitNumber, Instant startAt,
			Instant endAt, String disposition, CourseAttendanceStatus attendanceStatus, Instant attendanceRecordedAt) {}

	@Schema(additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
	public record RosterEntry(UUID enrollmentSessionId, String memberCode, String fullName,
			String profileImageUrl, CourseAttendanceStatus attendanceStatus, Instant attendanceRecordedAt) {}

	@Schema(additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
	public record AttendanceUpdate(CourseAttendanceStatus status) {}
}
