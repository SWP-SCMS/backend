package com.scms.backend.membership;

import java.math.BigInteger;
import java.time.Instant;
import java.util.UUID;

public record MembershipHistoryResponse(
	UUID id,
	UUID orderId,
	String planCode,
	String offerName,
	BigInteger priceAmount,
	String currencyCode,
	int durationDays,
	String status,
	Instant startsAt,
	Instant endsAt) {
}
