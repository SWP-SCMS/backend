package com.scms.backend.membership;

import java.math.BigInteger;
import java.util.UUID;

public record RefundRequestCreateRequest(UUID paymentId, UUID membershipId, BigInteger requestedAmount,
		String reason, String idempotencyKey) { }
