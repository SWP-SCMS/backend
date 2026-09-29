package com.scms.backend.auth;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import com.scms.backend.account.Account;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService {

	private final JwtEncoder jwtEncoder;
	private final AuthProperties properties;
	private final Clock clock;

	JwtTokenService(JwtEncoder jwtEncoder, AuthProperties properties, Clock clock) {
		this.jwtEncoder = jwtEncoder;
		this.properties = properties;
		this.clock = clock;
	}

	AccessToken issue(Account account) {
		Instant issuedAt = clock.instant();
		Instant expiresAt = issuedAt.plus(properties.accessTokenTtl());
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();
		JwtClaimsSet claims = JwtClaimsSet.builder()
			.subject(account.getId().toString())
			.issuedAt(issuedAt)
			.expiresAt(expiresAt)
			.id(UUID.randomUUID().toString())
			.claim("role", account.getRole().name())
			.build();
		String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
		return new AccessToken(token, properties.accessTokenTtl().toSeconds());
	}

	record AccessToken(String value, long expiresInSeconds) {
	}
}
