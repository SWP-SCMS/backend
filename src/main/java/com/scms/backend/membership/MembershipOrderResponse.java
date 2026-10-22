package com.scms.backend.membership;

import java.math.BigInteger;
import java.time.Instant;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

public record MembershipOrderResponse(
	UUID orderId,
	String orderNumber,
	String memberId,
	UUID offerId,
	String offerName,
	@Schema(pattern = "^[A-Z][A-Z0-9_]{1,49}$") String planCode,
	BigInteger priceAmount,
	String currencyCode,
	int durationDays,
	PaymentMethod paymentMethod,
	MembershipOrderStatus status,
	Instant createdAt,
	@Schema(required = true, requiredMode = Schema.RequiredMode.REQUIRED) FulfillmentMode fulfillmentMode) {
}
