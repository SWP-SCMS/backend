package com.scms.backend.emailverification.verification;

import java.time.Instant;

public record ChangePendingEmailResponse(boolean changed, String maskedEmail, String csrfToken, Instant otpExpiresAt, Instant resendAvailableAt, Instant registrationExpiresAt, Instant serverTime) {
}
