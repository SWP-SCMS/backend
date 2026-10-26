package com.scms.backend.emailverification;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
class EmailOutboxEnqueuer {
	static final String SNAPSHOT_FORMAT = "EMAIL_REQUEST_V1";
	private static final Duration RESEND_IDEMPOTENCY_RETENTION = Duration.ofHours(24);

	private final JdbcTemplate db;
	private final EmailVerificationProperties verification;
	private final EmailDeliveryProperties delivery;
	private final EmailVerificationCrypto crypto;
	private final EmailMessageRenderer renderer;
	private final EmailPayloadCodec codec;

	EmailOutboxEnqueuer(JdbcTemplate db, EmailVerificationProperties verification,
			EmailDeliveryProperties delivery, EmailVerificationCrypto crypto, EmailMessageRenderer renderer,
			EmailPayloadCodec codec) {
		this.db = db;
		this.verification = verification;
		this.delivery = delivery;
		this.crypto = crypto;
		this.renderer = renderer;
		this.codec = codec;
	}

	UUID enqueue(String eventKey, UUID accountId, int generation, String recipient, String templateKey,
			String otp, Instant deliverBefore, Instant now) {
		Duration window = Duration.between(now, deliverBefore);
		if (window.isNegative() || window.isZero())
			throw new IllegalArgumentException("Email delivery window must be positive");
		if (window.compareTo(RESEND_IDEMPOTENCY_RETENTION) >= 0)
			throw new IllegalArgumentException("Email delivery window must be shorter than 24 hours");
		String provider = delivery.getProvider();
		String recipientName = db.queryForObject("select full_name from accounts where id=?", String.class, accountId);
		long minutes = Math.max(1, (window.toSeconds() + 59) / 60);
		EmailMessage message = renderer.render(provider, templateKey, recipient, recipientName, otp, minutes, now);
		String payload = codec.serialize(message);
		String version = verification.getEncryptionActiveVersion();
		var encrypted = crypto.encryptPayload(payload, version);
		UUID outboxId = UUID.randomUUID();
		db.update("""
			insert into email_outbox(id,event_key,account_id,generation,recipient_email,template_key,deliver_before,
			 payload_ciphertext,payload_nonce,encryption_key_version,payload_format,delivery_provider,status,next_retry_at)
			values (?,?,?,?,?,?,?,?,?,?,?,?,'PENDING',?)
			""", outboxId, eventKey, accountId, generation, recipient, templateKey, Timestamp.from(deliverBefore),
			encrypted.ciphertext(), encrypted.nonce(), version, SNAPSHOT_FORMAT, provider, Timestamp.from(now));
		return outboxId;
	}

}
