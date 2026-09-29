package com.scms.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;

class RefreshTokenCookieServiceTests {

	@Test
	void productionConfigurationCreatesSecureCookieWithoutDomain() {
		AuthProperties properties = new AuthProperties("test-only-jwt-secret-at-least-32-bytes-long",
			Duration.ofMinutes(15), Duration.ofDays(7), true);
		RefreshTokenCookieService service = new RefreshTokenCookieService(properties);

		ResponseCookie cookie = service.create("opaque-refresh-token");

		assertThat(cookie.isHttpOnly()).isTrue();
		assertThat(cookie.isSecure()).isTrue();
		assertThat(cookie.getPath()).isEqualTo("/api/v1/auth");
		assertThat(cookie.getSameSite()).isEqualTo("Lax");
		assertThat(cookie.getDomain()).isNull();
		assertThat(cookie.getMaxAge()).isEqualTo(Duration.ofDays(7));
	}
}
