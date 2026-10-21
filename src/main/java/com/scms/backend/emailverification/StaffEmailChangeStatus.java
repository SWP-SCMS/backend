package com.scms.backend.emailverification;

import java.time.Instant;

public record StaffEmailChangeStatus(boolean pending, String maskedEmail, Instant otpExpiresAt,
		Instant resendAvailableAt) {
	public static StaffEmailChangeStatus none() { return new StaffEmailChangeStatus(false, null, null, null); }
}
