package com.scms.backend.auth;

import java.time.Instant;

import com.scms.backend.account.AccountStatus;

public record RegistrationResponse(boolean verificationRequired, AccountStatus status, String maskedEmail,
		String csrfToken, Instant otpExpiresAt, Instant resendAvailableAt, Instant registrationExpiresAt,
		Instant serverTime) {
}
