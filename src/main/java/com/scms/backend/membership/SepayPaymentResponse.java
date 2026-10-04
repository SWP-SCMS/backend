package com.scms.backend.membership;
import java.math.BigInteger; import java.time.Instant; import java.util.UUID;
public record SepayPaymentResponse(UUID paymentId,UUID orderId,String orderNumber,BigInteger amount,String currency,String provider,String paymentReference,String qrUrl,Instant expiresAt,String status) {}
