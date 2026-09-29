package com.scms.backend.auth;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("scms.security")
public record AuthProperties(String jwtSecret, Duration accessTokenTtl, Duration refreshTokenTtl,
		boolean refreshCookieSecure) {

	public AuthProperties {
		Objects.requireNonNull(jwtSecret, "JWT secret is required");
		Objects.requireNonNull(accessTokenTtl, "Access-token TTL is required");
		Objects.requireNonNull(refreshTokenTtl, "Refresh-token TTL is required");
		if (jwtSecret.getBytes(StandardCharsets.UTF_8).length < 32) {
			throw new IllegalArgumentException("JWT secret must contain at least 32 UTF-8 bytes");
		}
		if (accessTokenTtl.isZero() || accessTokenTtl.isNegative()) {
			throw new IllegalArgumentException("Access-token TTL must be positive");
		}
		if (refreshTokenTtl.isZero() || refreshTokenTtl.isNegative()) {
			throw new IllegalArgumentException("Refresh-token TTL must be positive");
		}
	}
}
