package com.scms.backend.membership;

import java.math.BigInteger;
import java.time.Instant;
import java.util.UUID;

public record ReconciliationQueueItem(UUID paymentId, UUID orderId, String orderNumber,
		UUID memberAccountId, String memberName, BigInteger amount, String currencyCode,
		String transferContent, Instant createdAt, Instant expiresAt) {
}
