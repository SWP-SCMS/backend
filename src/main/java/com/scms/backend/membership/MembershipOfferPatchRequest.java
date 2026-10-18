package com.scms.backend.membership;

import java.math.BigInteger;

record MembershipOfferPatchRequest(String planCode, String name, String description,
		BigInteger priceAmount, Integer durationDays, MembershipOfferStatus status) {
}
