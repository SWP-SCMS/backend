package com.scms.backend.course.withdrawal;

import com.scms.backend.course.CourseWithdrawalCause;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigInteger;
import java.time.Instant;
import java.util.UUID;

public final class CourseWithdrawalApiModels {

	private CourseWithdrawalApiModels() {
	}

	record WithdrawalCreate(String reason) {
	}

	record WithdrawalDecision(String reason) {
	}

	@Schema(description = "Immutable request-time refund snapshot; legacy snapshotSource identifies approval-time or unavailable values.")
	public record WithdrawalRequest(UUID id, UUID enrollmentId, UUID courseRunId, UUID memberAccountId, UUID requesterAccountId, @Schema(allowableValues = { "PENDING", "APPROVED", "REJECTED", "CANCELLED" }) String status, String requestReason, String decisionReason, CourseWithdrawalCause cause, boolean withdrawalEligible, String withdrawalReason, Instant requestedAt, UUID decidedByAccountId, Instant decidedAt, Integer totalUnits, Integer remainingUnits, BigInteger refundBasisVnd, BigInteger refundAmountVnd, UUID memberId, String memberCode, String memberName, UUID courseId, String courseCode, String courseName, String runCode, String courseRunStatus, BigInteger estimatedRefundAmount, UUID scheduleChangeResponseId, UUID changeEventId, @Schema(types = { "string", "null" }) Instant snapshotCapturedAt, @Schema(allowableValues = { "REQUEST_TIME", "LEGACY_APPROVAL", "UNAVAILABLE" }) String snapshotSource, @Schema(types = { "boolean", "null" }) Boolean snapshotWithdrawalEligible, @Schema(types = { "string", "null" }) String snapshotWithdrawalReason, @Schema(types = { "string", "null" }) Instant cancelledAt, @Schema(types = { "string", "null" }) UUID cancelledByAccountId, @Schema(types = { "string", "null" }) String cancellationReason, boolean canCancel, boolean learningSuspended) {
	}
}
