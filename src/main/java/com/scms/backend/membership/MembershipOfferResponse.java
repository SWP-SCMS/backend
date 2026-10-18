package com.scms.backend.membership;

import java.math.BigInteger;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

public record MembershipOfferResponse(
	UUID offerId,
	@Schema(pattern = "^[A-Z][A-Z0-9_]{1,49}$") String planCode,
	String planDisplayName,
	String name,
	String description,
	BigInteger priceAmount,
	String currencyCode,
	int durationDays,
	boolean supportsBooking,
	boolean supportsPersonalCoaching) {

	static MembershipOfferResponse from(MembershipOffer offer) {
		MembershipPlan plan = offer.getPlan();
		return new MembershipOfferResponse(offer.getId(), plan.getPlanCode(), plan.getDisplayName(), offer.getName(),
			offer.getDescription(), offer.getPriceAmount(), offer.getCurrencyCode(), offer.getDurationDays(),
			plan.supportsBooking(), plan.supportsPersonalCoaching());
	}
}
