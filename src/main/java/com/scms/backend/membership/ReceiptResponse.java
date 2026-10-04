package com.scms.backend.membership;

import java.math.BigInteger;
import java.time.Instant;
import java.util.UUID;

public record ReceiptResponse(UUID receiptId, String receiptNumber, UUID paymentId, UUID orderId,
		UUID memberAccountId, BigInteger amount, String currency, String paymentMethod, Instant issuedAt) { }
