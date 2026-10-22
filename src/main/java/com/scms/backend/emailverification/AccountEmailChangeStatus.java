package com.scms.backend.emailverification;

import java.time.Instant;

public record AccountEmailChangeStatus(boolean pending, String maskedEmail, Instant otpExpiresAt,
		Instant resendAvailableAt) {
	public static AccountEmailChangeStatus none() { return new AccountEmailChangeStatus(false, null, null, null); }
}
