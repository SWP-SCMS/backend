package com.scms.backend.emailverification;

public interface EmailProvider {
	void sendVerificationOtp(String recipient, String recipientName, String otp, long expiresInMinutes);

	default void sendPasswordResetOtp(String recipient, String recipientName, String otp, long expiresInMinutes) {
		sendVerificationOtp(recipient, recipientName, otp, expiresInMinutes);
	}
}
