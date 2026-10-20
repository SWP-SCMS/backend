package com.scms.backend.emailverification;

import java.time.Instant;

public class PendingRegistrationException extends RuntimeException {
	private final Instant otpExpiresAt; private final Instant resendAvailableAt; private final Instant registrationExpiresAt; private final Instant serverTime;
	public PendingRegistrationException(Instant otp, Instant resend, Instant registration, Instant now) {
		super("Registration is waiting for email verification."); otpExpiresAt=otp; resendAvailableAt=resend;
		registrationExpiresAt=registration; serverTime=now;
	}
	public Instant getOtpExpiresAt(){return otpExpiresAt;} public Instant getResendAvailableAt(){return resendAvailableAt;}
	public Instant getRegistrationExpiresAt(){return registrationExpiresAt;} public Instant getServerTime(){return serverTime;}
}
