package com.scms.backend.membership;

import java.math.BigInteger;
import java.util.Comparator;
import java.util.List;
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
	List<String> benefitCodes,
	List<MembershipBenefitResponse> benefits,
	boolean supportsBooking,
	boolean supportsPersonalCoaching) {

	static MembershipOfferResponse from(MembershipOffer offer) {
		MembershipPlan plan = offer.getPlan();
		List<MembershipBenefitResponse> benefits = plan.getBenefits().stream()
			.map(MembershipBenefitResponse::from)
			.sorted(Comparator.comparing(MembershipBenefitResponse::code))
			.toList();
		return new MembershipOfferResponse(offer.getId(), plan.getPlanCode(), plan.getDisplayName(), offer.getName(),
			offer.getDescription(), offer.getPriceAmount(), offer.getCurrencyCode(), offer.getDurationDays(),
			benefits.stream().map(MembershipBenefitResponse::code).toList(), benefits,
			plan.supportsBooking(), plan.supportsPersonalCoaching());
	}
}
