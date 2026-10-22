package com.scms.backend.emailverification;

import java.time.Duration;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
class PasswordResetCookieService {
	static final String COOKIE_NAME = "password_reset_session";
	static final String COOKIE_PATH = "/api/v1/auth/password-reset";
	private static final Duration TTL = Duration.ofMinutes(10);

	private final EmailVerificationProperties properties;

	PasswordResetCookieService(EmailVerificationProperties properties) { this.properties = properties; }

	ResponseCookie create(String value) {
		return base(value).maxAge(TTL).build();
	}

	ResponseCookie clear() {
		return base("").maxAge(Duration.ZERO).build();
	}

	private ResponseCookie.ResponseCookieBuilder base(String value) {
		return ResponseCookie.from(COOKIE_NAME, value)
			.httpOnly(true)
			.secure(properties.isCookieSecure())
			.sameSite(properties.getCookieSameSite())
			.path(COOKIE_PATH);
	}
}
