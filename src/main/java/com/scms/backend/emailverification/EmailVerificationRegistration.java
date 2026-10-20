package com.scms.backend.emailverification;

import java.time.Instant;

public record EmailVerificationRegistration(String sessionToken, String csrfToken, Instant otpExpiresAt,
		Instant resendAvailableAt, Instant registrationExpiresAt, Instant serverTime) { }
