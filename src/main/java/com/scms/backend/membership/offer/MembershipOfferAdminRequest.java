package com.scms.backend.membership.offer;

import com.scms.backend.membership.MembershipOfferStatus;
import java.math.BigInteger;

public record MembershipOfferAdminRequest(String planCode, String name, String description, BigInteger priceAmount, Integer durationDays, MembershipOfferStatus status) {
}
