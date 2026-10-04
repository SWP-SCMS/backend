package com.scms.backend.membership;

import java.time.Instant;
import java.util.UUID;

public record PaymentResultResponse(UUID paymentId, UUID orderId, String status, String method,
		UUID membershipId, UUID receiptId, Instant paidAt) { }
