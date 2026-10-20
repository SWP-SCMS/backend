package com.scms.backend.emailverification;

public interface EmailProvider {
	void sendVerificationOtp(String recipient, String otp, long expiresInMinutes);
}
