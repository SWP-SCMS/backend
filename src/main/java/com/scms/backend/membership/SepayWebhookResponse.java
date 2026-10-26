package com.scms.backend.membership;

import java.util.UUID;

public record SepayWebhookResponse(boolean success, boolean processed, SepayWebhookOutcome outcome,
	UUID paymentId, UUID orderId, String paymentStatus) { }
