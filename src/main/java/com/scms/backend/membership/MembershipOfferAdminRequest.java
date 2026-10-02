package com.scms.backend.membership;

import java.math.BigInteger;

public record MembershipOfferAdminRequest(MembershipPlanCode planCode, String name, String description,
		BigInteger priceAmount, Integer durationDays, MembershipOfferStatus status) {
}
