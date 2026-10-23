package com.scms.backend.membership;

import java.time.Instant;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

public record PaymentResultResponse(UUID paymentId, UUID orderId, String status, String method,
		UUID membershipId, UUID receiptId, Instant paidAt,
		@Schema(required = true, requiredMode = Schema.RequiredMode.REQUIRED) FulfillmentMode fulfillmentMode,
		String orderNumber, String memberCode, String memberName,
		@Schema(nullable = true) String paymentReference) {
	public PaymentResultResponse(UUID paymentId, UUID orderId, String status, String method,
			UUID membershipId, UUID receiptId, Instant paidAt, FulfillmentMode fulfillmentMode) {
		this(paymentId, orderId, status, method, membershipId, receiptId, paidAt, fulfillmentMode,
			null, null, null, null);
	}
	public PaymentResultResponse(UUID paymentId, UUID orderId, String status, String method,
			UUID membershipId, UUID receiptId, Instant paidAt) {
		this(paymentId, orderId, status, method, membershipId, receiptId, paidAt, null);
	}
}
