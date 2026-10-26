package com.scms.backend.emailverification;

import java.util.UUID;

record EmailDeliveryRequest(UUID outboxId, String payloadJson, EmailMessage message) { }
