package com.scms.backend.membership;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

public record SepayWebhookResponse(
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean success,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean processed,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED) SepayWebhookOutcome outcome,
	UUID paymentId, UUID orderId, String paymentStatus) { }
