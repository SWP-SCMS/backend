package com.scms.backend.membership.offer;

import com.scms.backend.membership.MembershipOfferStatus;
import java.math.BigInteger;

record MembershipOfferPatchRequest(String planCode, String name, String description, BigInteger priceAmount, Integer durationDays, MembershipOfferStatus status) {
}
