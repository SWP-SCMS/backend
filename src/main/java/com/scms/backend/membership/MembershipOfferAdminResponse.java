package com.scms.backend.membership;

import java.math.BigInteger;
import java.util.UUID;

record MembershipOfferAdminResponse(UUID offerId, MembershipPlanCode planCode, String name,
		String description, BigInteger priceAmount, String currencyCode, int durationDays,
		MembershipOfferStatus status) {
	static MembershipOfferAdminResponse from(MembershipOffer offer) {
		return new MembershipOfferAdminResponse(offer.getId(), offer.getPlan().getPlanCode(), offer.getName(),
			offer.getDescription(), offer.getPriceAmount(), offer.getCurrencyCode(), offer.getDurationDays(),
			offer.getStatus());
	}
}
