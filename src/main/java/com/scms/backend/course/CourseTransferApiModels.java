package com.scms.backend.course;

import java.math.BigInteger;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

final class CourseTransferApiModels {
	private CourseTransferApiModels() { }

	record TransferRequest(UUID id, UUID scheduleChangeResponseId, UUID sourceEnrollmentId, UUID sourceRunId,
		UUID memberAccountId, String status, UUID targetRunId, String reason, Instant requestedAt,
		UUID decidedByAccountId, Instant decidedAt) { }

	record ApproveTransfer(UUID targetRunId, String reason) { }
	record RejectTransfer(String reason) { }

	record TransferOffer(UUID id, UUID requestId, UUID sourceEnrollmentId, UUID sourceRunId, UUID targetRunId,
		UUID memberAccountId, BigInteger creditVnd, BigInteger targetTuitionVnd, BigInteger differenceVnd,
		Instant expiresAt, String status, UUID differenceOrderId, Instant acceptedAt) { }

	record TransferTarget(UUID runId, String runCode, String courseCode, String courseName,
		BigInteger tuitionVnd, int capacity, long usedSeats, Instant registrationClosesAt,
		Instant nextSessionAt) { }

	record TransferRequestPage(List<TransferRequest> content) { }
}
