package com.scms.backend.auth;

import java.time.Duration;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenCookieService {

	public static final String COOKIE_NAME = "refresh_token";
	public static final String COOKIE_PATH = "/api/v1/auth";

	private final AuthProperties properties;

	RefreshTokenCookieService(AuthProperties properties) {
		this.properties = properties;
	}

	ResponseCookie create(String token) {
		return baseCookie(token)
			.maxAge(properties.refreshTokenTtl())
			.build();
	}

	ResponseCookie clear() {
		return baseCookie("")
			.maxAge(Duration.ZERO)
			.build();
	}

	private ResponseCookie.ResponseCookieBuilder baseCookie(String value) {
		return ResponseCookie.from(COOKIE_NAME, value)
			.httpOnly(true)
			.secure(properties.refreshCookieSecure())
			.sameSite("Lax")
			.path(COOKIE_PATH);
	}
}
