package com.scms.backend.membership;

import java.math.BigInteger;
import java.time.Instant;
import java.util.UUID;

public record MembershipFreezeCashPaymentResponse(UUID requestId, UUID paymentId, BigInteger amount, String currencyCode,
		MembershipFreezeRequestStatus status, Instant paidAt) { }
