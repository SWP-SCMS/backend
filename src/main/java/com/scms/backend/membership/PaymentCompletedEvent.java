package com.scms.backend.membership;

import java.util.UUID;

record PaymentCompletedEvent(UUID memberAccountId, UUID paymentId, UUID orderId) { }
