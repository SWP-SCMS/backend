package com.scms.backend.auth;

import com.scms.backend.account.AccountStatus;
import java.time.Instant;

public record RegistrationResponse(boolean verificationRequired, AccountStatus status, String maskedEmail, String csrfToken, Instant otpExpiresAt, Instant resendAvailableAt, Instant registrationExpiresAt, Instant serverTime) {
}
