package com.scms.backend.emailverification;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.scms.backend.audit.AuditEvent;
import com.scms.backend.audit.AuditEventRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
class EmailOutboxDeliveryService {
	private static final int MAX_ATTEMPTS = 4;
	private static final long[] RETRY_DELAYS_SECONDS = {30, 120, 300};

	private final JdbcTemplate db;
	private final EmailVerificationCrypto crypto;
	private final EmailMessageRenderer renderer;
	private final EmailProviderRouter providers;
	private final EmailPayloadCodec codec;
	private final AuditEventRepository audits;

	EmailOutboxDeliveryService(JdbcTemplate db, EmailVerificationCrypto crypto, EmailMessageRenderer renderer,
			EmailProviderRouter providers, EmailPayloadCodec codec, AuditEventRepository audits) {
		this.db = db;
		this.crypto = crypto;
		this.renderer = renderer;
		this.providers = providers;
		this.codec = codec;
		this.audits = audits;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	Optional<EmailJob> claim(Instant now, String worker) {
		List<EmailJob> rows = db.query("""
			with candidate as (
			 select id from email_outbox where
			 ((status in ('PENDING','FAILED') and next_retry_at<=?) or (status='PROCESSING' and locked_at<?))
			 and deliver_before>? order by created_at for update skip locked limit 1)
			update email_outbox e set status='PROCESSING',locked_at=?,locked_by=?,retry_count=retry_count+1,updated_at=?
			from candidate c where e.id=c.id returning e.id,e.account_id,e.recipient_email,e.template_key,e.deliver_before,
			 e.payload_ciphertext,e.payload_nonce,e.encryption_key_version,e.retry_count,e.payload_format,e.delivery_provider
			""", this::map, Timestamp.from(now), Timestamp.from(now.minus(Duration.ofMinutes(5))), Timestamp.from(now),
			Timestamp.from(now), worker, Timestamp.from(now));
		return rows.stream().findFirst().map(job -> job.withRecipientName(db.queryForObject(
			"select full_name from accounts where id=?", String.class, job.accountId())));
	}

	EmailDeliveryResult deliver(EmailJob job, Instant now) {
		String decrypted = crypto.decryptPayload(job.ciphertext(), job.nonce(), job.keyVersion());
		String payload;
		EmailMessage message;
		if (EmailOutboxEnqueuer.SNAPSHOT_FORMAT.equals(job.payloadFormat())) {
			payload = decrypted;
			message = codec.deserialize(payload);
		} else {
			long minutes = Math.max(1, (Duration.between(now, job.deliverBefore()).toSeconds() + 59) / 60);
			message = renderer.render(job.templateKey(), job.recipient(),
				job.recipientName(), decrypted, minutes, now);
			payload = codec.serialize(message);
		}
		return providers.send(job.deliveryProvider(), new EmailDeliveryRequest(job.id(), payload, message));
	}

	OutboxMetrics metrics(Instant now) {
		return db.queryForObject("""
			select count(*) filter (where status='FAILED'),
			 count(*) filter (where status='PENDING' and created_at<?),
			 count(*) filter (where status='PROCESSING' and locked_at<?),
			 count(*) filter (where status='TERMINAL_FAILED' and updated_at>=?),
			 count(*) filter (where status in ('PENDING','FAILED','PROCESSING') and created_at<?),
			 count(*) filter (where status='SENT' and sent_at>=?),
			 coalesce(max(extract(epoch from (? - created_at))) filter
				(where status in ('PENDING','FAILED','PROCESSING')), 0)
			 from email_outbox
			""", (rs, row) -> new OutboxMetrics(rs.getLong(1), rs.getLong(2), rs.getLong(3),
				rs.getLong(4), rs.getLong(5), rs.getLong(6), rs.getLong(7)), Timestamp.from(now.minus(Duration.ofMinutes(2))),
			Timestamp.from(now.minus(Duration.ofMinutes(5))), Timestamp.from(now.minus(Duration.ofHours(24))),
			Timestamp.from(now.minus(Duration.ofMinutes(10))), Timestamp.from(now.minus(Duration.ofHours(24))),
			Timestamp.from(now));
	}

	static java.util.List<String> alertLevels(OutboxMetrics metrics) {
		var levels = new java.util.ArrayList<String>(2);
		if (metrics.failed() > 0 || metrics.pendingOlderThan2m() > 0 || metrics.processingOlderThan5m() > 0)
			levels.add("WARN");
		if (metrics.terminalFailedRecent() > 0 || metrics.activeOlderThan10m() > 0 || metrics.sentLast24h() >= 40)
			levels.add("ERROR");
		return java.util.List.copyOf(levels);
	}

	record OutboxMetrics(long failed, long pendingOlderThan2m, long processingOlderThan5m,
		long terminalFailedRecent, long activeOlderThan10m, long sentLast24h, long oldestActiveAgeSeconds) { }

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	void sent(UUID id, String providerMessageId, Instant now) {
		db.update("""
			update email_outbox set status='SENT',sent_at=?,provider_message_id=?,payload_ciphertext=null,
			 payload_nonce=null,locked_at=null,locked_by=null,last_error=null,updated_at=? where id=?
			""", Timestamp.from(now), providerMessageId, Timestamp.from(now), id);
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	void failed(EmailJob job, Instant now, String errorName, Duration providerRetryAfter) {
		Duration retryDelay = retryDelay(job.retryCount(), providerRetryAfter);
		if (retryDelay == null) {
			terminal(job.id(), now, "DELIVERY_ATTEMPTS_EXHAUSTED");
			return;
		}
		Instant next = now.plus(retryDelay);
		if (!next.plusSeconds(10).isBefore(job.deliverBefore())) {
			terminal(job.id(), now, "DELIVERY_WINDOW_EXHAUSTED");
			return;
		}
		db.update("""
			update email_outbox set status='FAILED',next_retry_at=?,locked_at=null,locked_by=null,last_error=?,updated_at=?
			where id=?
			""", Timestamp.from(next), errorName, Timestamp.from(now), job.id());
	}

	static Duration retryDelay(int attempt, Duration providerRetryAfter) {
		if (attempt >= MAX_ATTEMPTS) return null;
		if (attempt < 1) throw new IllegalArgumentException("Attempt must be positive");
		long defaultDelay = RETRY_DELAYS_SECONDS[attempt - 1];
		long providerDelay = providerRetryAfter == null ? 0 : Math.max(0, providerRetryAfter.toSeconds());
		return Duration.ofSeconds(Math.max(defaultDelay, providerDelay));
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	void terminal(UUID id, Instant now, String reason) {
		List<UUID> accounts = db.query("""
			update email_outbox set status='TERMINAL_FAILED',payload_ciphertext=null,payload_nonce=null,locked_at=null,
			 locked_by=null,last_error=?,updated_at=? where id=? and status<>'TERMINAL_FAILED' returning account_id
			""", (rs, n) -> rs.getObject(1, UUID.class), reason, Timestamp.from(now), id);
		accounts.forEach(account -> auditTerminal(account, id, reason));
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	int expire(Instant now) {
		var rows = db.query("""
			update email_outbox set status='TERMINAL_FAILED',payload_ciphertext=null,payload_nonce=null,locked_at=null,
			 locked_by=null,last_error='OTP_EXPIRED',updated_at=? where status in ('PENDING','FAILED','PROCESSING')
			 and deliver_before<=? returning id,account_id
			""", (rs, n) -> new TerminalJob(rs.getObject(1, UUID.class), rs.getObject(2, UUID.class)),
			Timestamp.from(now), Timestamp.from(now));
		rows.forEach(job -> auditTerminal(job.accountId(), job.id(), "OTP_EXPIRED"));
		return rows.size();
	}

	private void auditTerminal(UUID accountId, UUID outboxId, String reason) {
		audits.save(new AuditEvent(UUID.randomUUID(), "EMAIL_DELIVERY_TERMINAL_FAILED", "ACCOUNT", accountId,
			java.util.Map.of("outboxId", outboxId.toString(), "reason", reason)));
	}

	private EmailJob map(ResultSet rs, int n) throws SQLException {
		return new EmailJob(rs.getObject(1, UUID.class), rs.getObject(2, UUID.class), rs.getString(3), rs.getString(4),
			rs.getTimestamp(5).toInstant(), rs.getBytes(6), rs.getBytes(7), rs.getString(8), rs.getInt(9),
			rs.getString(10), rs.getString(11), null);
	}

	record EmailJob(UUID id, UUID accountId, String recipient, String templateKey, Instant deliverBefore,
		byte[] ciphertext, byte[] nonce, String keyVersion, int retryCount, String payloadFormat,
		String deliveryProvider, String recipientName) {
		EmailJob withRecipientName(String value) { return new EmailJob(id, accountId, recipient, templateKey,
			deliverBefore, ciphertext, nonce, keyVersion, retryCount, payloadFormat, deliveryProvider, value); }
	}

	private record TerminalJob(UUID id, UUID accountId) { }
}
