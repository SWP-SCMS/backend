package com.scms.backend.course;

import java.math.BigInteger;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

final class CourseRefundApiModels {
	private CourseRefundApiModels() { }

	record RefundDecision(Long expectedVersion, String reason) { }
	record RefundDisbursement(Long expectedVersion, BigInteger amountVnd, String method,
		String reference, String evidenceReference) { }
	record RefundVerification(Long expectedVersion, BigInteger amountVnd, String disbursementReference,
		String evidenceReference, String reason) { }
	record RefundTerminal(Long expectedVersion, String reason) { }
	record RefundObligation(UUID id, UUID enrollmentId, UUID courseRunId, UUID memberAccountId,
		UUID courseOrderId, UUID coursePaymentId, String source, String status, BigInteger grossObligationVnd,
		BigInteger approvedAmountVnd, BigInteger disbursedAmountVnd, BigInteger verifiedAmountVnd,
		String currencyCode, UUID requesterAccountId, String requestReason, UUID approvedByAccountId,
		String approvalReason, Instant approvedAt, UUID rejectedByAccountId, String rejectionReason,
		Instant rejectedAt, UUID disbursedByAccountId, Instant disbursedAt, String disbursementMethod,
		String disbursementReference, String evidenceReference, UUID verifiedByAccountId,
		String verificationReason, Instant verifiedAt, UUID terminalByAccountId, String terminalReason,
		Instant terminalAt, Instant createdAt, Instant updatedAt, long version) { }
	record RefundObligationPage(List<RefundObligation> items, int page, int size, long totalElements,
		int totalPages) { }
	record MemberRefundObligation(UUID id, UUID enrollmentId, UUID courseRunId, String source, String status,
		BigInteger grossObligationVnd, BigInteger approvedAmountVnd, BigInteger disbursedAmountVnd,
		BigInteger verifiedAmountVnd, String currencyCode, Instant createdAt, Instant updatedAt, long version) { }
	record MemberRefundObligationPage(List<MemberRefundObligation> items, int page, int size,
		long totalElements, int totalPages) { }
}
