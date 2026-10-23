package com.scms.backend.membership;

import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record RefundRequestResponse(UUID id, UUID paymentId, UUID orderId, UUID membershipId, UUID receiptId,
		UUID memberAccountId, RefundType type, RefundRequestStatus status, BigInteger requestedAmount,
		BigInteger suggestedAmount, BigInteger approvedAmount, Integer remainingDaysSnapshot,
			LocalDate calculationDate, String reason, String rejectionReason, String disbursementEvidence, Instant requestedAt,
		Instant firstApprovedAt, Instant cashDisbursedAt, Instant completedAt, Instant rejectedAt,
		Long version) {
	static RefundRequestResponse from(RefundRequest request) {
		return new RefundRequestResponse(request.getId(), request.getPaymentId(), request.getOrderId(),
			request.getMembershipId(), request.getReceiptId(), request.getMemberAccountId(), request.getType(),
			request.getStatus(), request.getRequestedAmount(), request.getSuggestedAmount(), request.getApprovedAmount(),
			request.getRemainingDaysSnapshot(), request.getCalculationDate(), request.getReason(), request.getRejectionReason(),
			request.getDisbursementEvidence(), request.getRequestedAt(), request.getFirstApprovedAt(),
			request.getCashDisbursedAt(), request.getCompletedAt(), request.getRejectedAt(), request.getVersion());
	}
}
