package com.scms.backend.membership.payment;

import com.scms.backend.membership.SepayWebhookOutcome;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

public record SepayWebhookResponse(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean success, @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean processed, @Schema(requiredMode = Schema.RequiredMode.REQUIRED) SepayWebhookOutcome outcome, UUID paymentId, UUID orderId, String paymentStatus) {
}
