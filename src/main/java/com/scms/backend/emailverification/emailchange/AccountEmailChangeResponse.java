package com.scms.backend.emailverification.emailchange;

import java.time.Instant;

public record AccountEmailChangeResponse(boolean pending, String maskedEmail, Instant otpExpiresAt, Instant resendAvailableAt, Instant serverTime) {
}
