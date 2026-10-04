package com.scms.backend.membership;

import java.time.Instant;
import java.util.UUID;

public record MembershipOrderCancellationResponse(UUID orderId, String status, int cancelledPayments,
		Instant cancelledAt, String reason) {
}
