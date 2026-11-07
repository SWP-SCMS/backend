package com.scms.backend.course;

import java.time.Instant;
import java.math.BigInteger;
import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

public final class CourseEnrollmentApiModels {
	private CourseEnrollmentApiModels() {}

	@Schema(additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
	public record MemberEnrollment(UUID enrollmentId, UUID courseRunId, String courseCode, String courseName,
			String runCode, @Schema(requiredMode = Schema.RequiredMode.REQUIRED) CourseRunStatus courseRunStatus,
			String lifecycleStatus, Instant enrolledAt,
			@Schema(requiredMode = Schema.RequiredMode.REQUIRED) MemberPaymentSummary payment, List<MemberSession> sessions, boolean learningSuspended) {}

	@Schema(additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
	public record MemberPaymentSummary(
			@Schema(requiredMode = Schema.RequiredMode.REQUIRED) BigInteger amountVnd,
			@Schema(requiredMode = Schema.RequiredMode.REQUIRED, types = { "string", "null" }) String status,
			@Schema(requiredMode = Schema.RequiredMode.REQUIRED, types = { "string", "null" }) String method) {}

	@Schema(additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
	public record MemberSession(UUID enrollmentSessionId, UUID courseSessionId, int unitNumber, Instant startAt,
			Instant endAt, String disposition,
			@Schema(requiredMode = Schema.RequiredMode.REQUIRED) CourseSessionStatus sessionStatus,
			CourseAttendanceStatus attendanceStatus, Instant attendanceRecordedAt,
			@Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean isMakeup,
			@Schema(requiredMode = Schema.RequiredMode.REQUIRED, types = { "string", "null" }) UUID originalSessionId,
			@Schema(requiredMode = Schema.RequiredMode.REQUIRED, types = { "string", "null" }) UUID replacementSessionId,
			String roomName, String coachName) {}

	@Schema(additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
	public record RosterEntry(UUID enrollmentSessionId, String memberCode, String fullName,
			String profileImageUrl, CourseAttendanceStatus attendanceStatus, Instant attendanceRecordedAt) {}

	@Schema(additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
	public record AttendanceUpdate(CourseAttendanceStatus status) {}

	@Schema(additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
	public record ReceptionCourseEnrollment(UUID memberId, String memberCode, String memberName, UUID courseId,
		String courseCode, String courseName, UUID courseRunId, String runCode, String courseRunStatus,
		UUID enrollmentId, String enrollmentStatus, Instant enrolledAt, int totalUnits, int remainingUnits,
		boolean withdrawalEligible, CourseWithdrawalEligibilityReason withdrawalReason,
		@Schema(description = "Null when withdrawalEligible is false.", nullable = true) BigInteger estimatedRefundAmount, boolean learningSuspended) {}

	@Schema(additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
	public record ReceptionCourseEnrollmentPage(List<ReceptionCourseEnrollment> content, int page, int size,
		long totalElements, int totalPages) {}
}
