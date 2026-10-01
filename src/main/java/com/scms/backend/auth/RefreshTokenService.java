package com.scms.backend.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountStatus;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshTokenService {

	private static final int TOKEN_BYTES = 32;

	private final RefreshTokenRepository repository;
	private final AuthProperties properties;
	private final Clock clock;
	private final SecureRandom secureRandom = new SecureRandom();

	RefreshTokenService(RefreshTokenRepository repository, AuthProperties properties, Clock clock) {
		this.repository = repository;
		this.properties = properties;
		this.clock = clock;
	}

	@Transactional
	IssuedRefreshToken issue(Account account) {
		return create(account, clock.instant());
	}

	@Transactional
	RotatedRefreshToken rotate(String rawToken) {
		if (rawToken == null || rawToken.isBlank()) {
			throw new InvalidRefreshTokenException();
		}
		RefreshToken current = repository.findByTokenHashForUpdate(hash(rawToken))
			.orElseThrow(InvalidRefreshTokenException::new);
		Instant now = clock.instant();
		if (!current.isUsableAt(now) || current.getAccount().getStatus() != AccountStatus.ACTIVE) {
			throw new InvalidRefreshTokenException();
		}

		IssuedRefreshToken replacement = create(current.getAccount(), now);
		current.revoke(now, replacement.id());
		return new RotatedRefreshToken(current.getAccount(), replacement.value());
	}

	@Transactional
	void revokeIfPresent(String rawToken) {
		if (rawToken == null || rawToken.isBlank()) {
			return;
		}
		repository.findByTokenHashForUpdate(hash(rawToken))
			.ifPresent(token -> token.revoke(clock.instant(), null));
	}

	@Transactional
	void revokeIfPresent(String rawToken, UUID accountId) {
		if (rawToken == null || rawToken.isBlank()) {
			return;
		}
		Instant now = clock.instant();
		repository.findByTokenHashForUpdate(hash(rawToken))
			.filter(token -> token.getAccount().getId().equals(accountId))
			.filter(token -> token.isUsableAt(now))
			.ifPresent(token -> token.revoke(now, null));
	}

	@Transactional
	public void revokeAll(UUID accountId) {
		Instant now = clock.instant();
		repository.findAllByAccountId(accountId).forEach(token -> token.revoke(now, null));
	}

	private IssuedRefreshToken create(Account account, Instant now) {
		String rawToken = generateToken();
		RefreshToken token = new RefreshToken(UUID.randomUUID(), account, hash(rawToken),
			now.plus(properties.refreshTokenTtl()));
		repository.save(token);
		return new IssuedRefreshToken(token.getId(), rawToken);
	}

	private String generateToken() {
		byte[] bytes = new byte[TOKEN_BYTES];
		secureRandom.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	private String hash(String rawToken) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
		}
		catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 is unavailable", exception);
		}
	}

	record IssuedRefreshToken(UUID id, String value) {
	}

	record RotatedRefreshToken(Account account, String value) {
	}
}
