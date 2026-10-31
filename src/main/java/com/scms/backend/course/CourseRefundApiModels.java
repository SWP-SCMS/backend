package com.scms.backend.course;

import java.math.BigInteger;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

final class CourseRefundApiModels {
	private CourseRefundApiModels() { }

	record RefundDecision(Long expectedVersion, String reason) { }
	record RefundObligation(UUID id, UUID enrollmentId, UUID courseRunId, UUID memberAccountId,
		UUID courseOrderId, UUID coursePaymentId, String source, String status, BigInteger grossObligationVnd,
		BigInteger approvedAmountVnd, BigInteger disbursedAmountVnd, BigInteger verifiedAmountVnd,
		String currencyCode, UUID requesterAccountId, String requestReason, UUID approvedByAccountId,
		String approvalReason, Instant approvedAt, UUID rejectedByAccountId, String rejectionReason,
		Instant rejectedAt, String evidenceReference, Instant createdAt, Instant updatedAt, long version) { }
	record RefundObligationPage(List<RefundObligation> items, int page, int size, long totalElements,
		int totalPages) { }
}
