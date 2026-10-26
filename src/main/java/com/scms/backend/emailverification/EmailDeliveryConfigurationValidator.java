package com.scms.backend.emailverification;

import java.time.Duration;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

@Component
class EmailDeliveryConfigurationValidator {
	private static final Duration RESEND_IDEMPOTENCY_RETENTION = Duration.ofHours(24);
	private final EmailDeliveryProperties delivery;
	private final EmailVerificationProperties verification;

	EmailDeliveryConfigurationValidator(EmailDeliveryProperties delivery, EmailVerificationProperties verification) {
		this.delivery = delivery;
		this.verification = verification;
	}

	@PostConstruct
	void validate() {
		if (delivery.getProvider().equals("resend")
				&& verification.getOtpTtl().compareTo(RESEND_IDEMPOTENCY_RETENTION) >= 0)
			throw new IllegalStateException("EMAIL_OTP_TTL must be shorter than Resend's 24-hour idempotency retention");
	}
}
