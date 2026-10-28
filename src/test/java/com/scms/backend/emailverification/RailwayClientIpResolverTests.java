package com.scms.backend.emailverification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

class RailwayClientIpResolverTests {
	@Test
	void trustsValidRailwayRealIpOnlyWhenProxyHeadersAreEnabled() {
		HttpServletRequest request = request("203.0.113.8", "10.0.0.4");

		assertThat(new RailwayClientIpResolver(false).resolve(request)).isEqualTo("10.0.0.4");
		assertThat(new RailwayClientIpResolver(true).resolve(request)).isEqualTo("203.0.113.8");
	}

	@Test
	void fallsBackToRemoteAddressWhenRealIpIsMissingOrInvalid() {
		var resolver = new RailwayClientIpResolver(true);
		assertThat(resolver.resolve(request(null, "10.0.0.4"))).isEqualTo("10.0.0.4");
		assertThat(resolver.resolve(request("attacker-controlled", "10.0.0.4"))).isEqualTo("10.0.0.4");
	}

	private HttpServletRequest request(String realIp, String remoteAddress) {
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getHeader("X-Real-IP")).thenReturn(realIp);
		when(request.getRemoteAddr()).thenReturn(remoteAddress);
		return request;
	}
}
