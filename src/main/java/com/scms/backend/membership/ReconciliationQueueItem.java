package com.scms.backend.membership;

import java.math.BigInteger;
import java.time.Instant;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

public record ReconciliationQueueItem(UUID paymentId, UUID orderId, String orderNumber,
		UUID memberAccountId, String memberName, BigInteger amount, String currencyCode,
		String transferContent, Instant createdAt, Instant expiresAt, String memberCode,
		@Schema(nullable = true) String paymentReference) {
	public ReconciliationQueueItem(UUID paymentId, UUID orderId, String orderNumber,
			UUID memberAccountId, String memberName, BigInteger amount, String currencyCode,
			String transferContent, Instant createdAt, Instant expiresAt) {
		this(paymentId, orderId, orderNumber, memberAccountId, memberName, amount, currencyCode,
			transferContent, createdAt, expiresAt, null, null);
	}
}
