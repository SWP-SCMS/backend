package com.scms.backend.auth;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.scms.backend.account.Account;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

	@Id
	@Column(nullable = false, updatable = false)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "account_id", nullable = false, updatable = false)
	private Account account;

	@Column(name = "token_hash", nullable = false, length = 64, updatable = false)
	private String tokenHash;

	@Generated(event = EventType.INSERT)
	@Column(name = "created_at", nullable = false, insertable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "expires_at", nullable = false, updatable = false)
	private Instant expiresAt;

	@Column(name = "revoked_at")
	private Instant revokedAt;

	@Column(name = "replaced_by_token_id")
	private UUID replacedByTokenId;

	@Version
	@Column(nullable = false)
	private Long version;

	protected RefreshToken() {
	}

	RefreshToken(UUID id, Account account, String tokenHash, Instant expiresAt) {
		this.id = Objects.requireNonNull(id);
		this.account = Objects.requireNonNull(account);
		this.tokenHash = Objects.requireNonNull(tokenHash);
		this.expiresAt = Objects.requireNonNull(expiresAt);
	}

	boolean isUsableAt(Instant now) {
		return revokedAt == null && expiresAt.isAfter(now);
	}

	void revoke(Instant now, UUID replacementTokenId) {
		if (revokedAt == null) {
			revokedAt = Objects.requireNonNull(now);
			replacedByTokenId = replacementTokenId;
		}
	}

	public UUID getId() {
		return id;
	}

	public Account getAccount() {
		return account;
	}

	public String getTokenHash() {
		return tokenHash;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getExpiresAt() {
		return expiresAt;
	}

	public Instant getRevokedAt() {
		return revokedAt;
	}

	public UUID getReplacedByTokenId() {
		return replacedByTokenId;
	}

	public Long getVersion() {
		return version;
	}
}
