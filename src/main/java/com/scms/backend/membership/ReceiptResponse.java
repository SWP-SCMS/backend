package com.scms.backend.membership;

import java.math.BigInteger;
import java.time.Instant;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

public record ReceiptResponse(UUID receiptId, String receiptNumber, UUID paymentId, UUID orderId,
		String planCode, String offerName, UUID memberAccountId, String memberName, String memberCode, BigInteger amount,
		String currency, String paymentMethod, Instant issuedAt, String orderNumber,
		@Schema(nullable = true) String paymentReference) {
	public ReceiptResponse(UUID receiptId, String receiptNumber, UUID paymentId, UUID orderId,
			String planCode, String offerName, UUID memberAccountId, String memberName, String memberCode,
			BigInteger amount, String currency, String paymentMethod, Instant issuedAt) {
		this(receiptId, receiptNumber, paymentId, orderId, planCode, offerName, memberAccountId, memberName,
			memberCode, amount, currency, paymentMethod, issuedAt, null, null);
	}
}
