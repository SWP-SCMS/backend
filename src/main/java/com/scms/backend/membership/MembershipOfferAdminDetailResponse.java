package com.scms.backend.membership;

import java.math.BigInteger;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

record MembershipOfferAdminDetailResponse(UUID offerId, String planCode, String planDisplayName, String name,
		String description, BigInteger priceAmount, String currencyCode, int durationDays,
		MembershipOfferStatus status, List<String> benefitCodes, List<MembershipBenefitResponse> benefits) {
	static MembershipOfferAdminDetailResponse from(MembershipOffer offer) {
		List<MembershipBenefitResponse> benefits = offer.getPlan().getBenefits().stream()
			.map(MembershipBenefitResponse::from)
			.sorted(Comparator.comparing(MembershipBenefitResponse::code))
			.toList();
		return new MembershipOfferAdminDetailResponse(offer.getId(), offer.getPlan().getPlanCode(),
			offer.getPlan().getDisplayName(), offer.getName(), offer.getDescription(), offer.getPriceAmount(),
			offer.getCurrencyCode(), offer.getDurationDays(), offer.getStatus(),
			benefits.stream().map(MembershipBenefitResponse::code).toList(), benefits);
	}
}
