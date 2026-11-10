package com.scms.backend.emailverification.delivery;

import java.util.UUID;

record EmailDeliveryRequest(UUID outboxId, String payloadJson, EmailMessage message) {
}
