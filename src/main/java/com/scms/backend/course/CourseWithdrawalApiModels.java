package com.scms.backend.course;

import java.math.BigInteger;
import java.time.Instant;
import java.util.UUID;

final class CourseWithdrawalApiModels {
	private CourseWithdrawalApiModels() { }

	record WithdrawalCreate(String reason) { }
	record WithdrawalDecision(String reason) { }
	record WithdrawalRequest(UUID id, UUID enrollmentId, UUID courseRunId, UUID memberAccountId,
		UUID requesterAccountId, String status, String requestReason, String decisionReason,
		CourseWithdrawalCause cause, boolean withdrawalEligible, String withdrawalReason,
		Instant requestedAt, UUID decidedByAccountId, Instant decidedAt, Integer totalUnits,
		Integer remainingUnits, BigInteger refundBasisVnd, BigInteger refundAmountVnd,
		UUID memberId, String memberCode, String memberName, UUID courseId, String courseCode, String courseName,
		String runCode, String courseRunStatus, BigInteger estimatedRefundAmount,
		UUID scheduleChangeResponseId, UUID changeEventId) { }
}
