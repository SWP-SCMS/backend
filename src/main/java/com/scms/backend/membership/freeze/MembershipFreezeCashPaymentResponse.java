package com.scms.backend.membership.freeze;

import com.scms.backend.membership.MembershipFreezeRequestStatus;
import java.math.BigInteger;
import java.time.Instant;
import java.util.UUID;

public record MembershipFreezeCashPaymentResponse(UUID requestId, UUID paymentId, BigInteger amount, String currencyCode, MembershipFreezeRequestStatus status, Instant paidAt) {
}
