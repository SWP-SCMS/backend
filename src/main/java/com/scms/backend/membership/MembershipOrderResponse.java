package com.scms.backend.membership;

import java.math.BigInteger;
import java.time.Instant;
import java.util.UUID;

public record MembershipOrderResponse(
	UUID orderId,
	String orderNumber,
	String memberId,
	UUID offerId,
	String offerName,
	MembershipPlanCode planCode,
	BigInteger priceAmount,
	String currencyCode,
	int durationDays,
	PaymentMethod paymentMethod,
	MembershipOrderStatus status,
	Instant createdAt) {
}
