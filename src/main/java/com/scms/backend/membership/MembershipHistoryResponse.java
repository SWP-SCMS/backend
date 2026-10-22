package com.scms.backend.membership;

import java.math.BigInteger;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

public record MembershipHistoryResponse(
	UUID id,
	UUID orderId,
	String planCode,
	String offerName,
	BigInteger priceAmount,
	String currencyCode,
	int durationDays,
	@Schema(allowableValues = { "RESERVED", "ACTIVE", "EXPIRED", "TERMINATED" }) String status,
	@Schema(nullable = true) Instant startsAt,
	@Schema(nullable = true) Instant endsAt,
	List<String> benefitCodes,
	List<MembershipBenefitResponse> benefits) {

	MembershipHistoryResponse(UUID id, UUID orderId, String planCode, String offerName, BigInteger priceAmount,
			String currencyCode, int durationDays, String status, Instant startsAt, Instant endsAt) {
		this(id, orderId, planCode, offerName, priceAmount, currencyCode, durationDays, status, startsAt, endsAt,
			List.of(), List.of());
	}
}
