package com.scms.backend.membership;

import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

public record RefundRequestResponse(UUID id, UUID paymentId, UUID orderId, UUID membershipId, UUID receiptId,
		UUID memberAccountId, RefundType type, RefundRequestStatus status, BigInteger requestedAmount,
		BigInteger suggestedAmount, BigInteger approvedAmount, Integer remainingDaysSnapshot,
			LocalDate calculationDate, String reason, String rejectionReason, String disbursementEvidence, Instant requestedAt,
		Instant firstApprovedAt, Instant cashDisbursedAt, Instant completedAt, Instant rejectedAt,
		Long version,
		@Schema(nullable = true) String memberCode,
		@Schema(nullable = true) String memberFullName,
		@Schema(nullable = true) String orderNumber,
		@Schema(nullable = true) String paymentReference,
		@Schema(nullable = true) String receiptNumber) {
	static RefundRequestResponse from(RefundRequest request, Map<String, Object> display) {
		return new RefundRequestResponse(request.getId(), request.getPaymentId(), request.getOrderId(),
			request.getMembershipId(), request.getReceiptId(), request.getMemberAccountId(), request.getType(),
			request.getStatus(), request.getRequestedAmount(), request.getSuggestedAmount(), request.getApprovedAmount(),
			request.getRemainingDaysSnapshot(), request.getCalculationDate(), request.getReason(), request.getRejectionReason(),
			request.getDisbursementEvidence(), request.getRequestedAt(), request.getFirstApprovedAt(),
			request.getCashDisbursedAt(), request.getCompletedAt(), request.getRejectedAt(), request.getVersion(),
			(String) display.get("member_code"), (String) display.get("member_full_name"),
			(String) display.get("order_number"), (String) display.get("payment_reference"), (String) display.get("receipt_number"));
	}
}
