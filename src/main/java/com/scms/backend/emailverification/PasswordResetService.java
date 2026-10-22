package com.scms.backend.emailverification;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.audit.AuditEvent;
import com.scms.backend.audit.AuditEventRepository;
import com.scms.backend.auth.RefreshTokenService;
import com.scms.backend.notification.NotificationWriter;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasswordResetService {
	private static final Duration SESSION_TTL = Duration.ofMinutes(10);
	private static final String TEMPLATE_KEY = "PASSWORD_RESET_OTP";

	private final JdbcTemplate db;
	private final AccountRepository accounts;
	private final PasswordEncoder passwords;
	private final RefreshTokenService refreshTokens;
	private final EmailVerificationProperties properties;
	private final EmailVerificationCrypto crypto;
	private final DatabaseRateLimiter limits;
	private final AuditEventRepository audits;
	private final NotificationWriter notifications;
	private final Clock clock;

	PasswordResetService(JdbcTemplate db, AccountRepository accounts, PasswordEncoder passwords,
			RefreshTokenService refreshTokens, EmailVerificationProperties properties,
			EmailVerificationCrypto crypto, DatabaseRateLimiter limits, AuditEventRepository audits,
			NotificationWriter notifications, Clock clock) {
		this.db = db;
		this.accounts = accounts;
		this.passwords = passwords;
		this.refreshTokens = refreshTokens;
		this.properties = properties;
		this.crypto = crypto;
		this.limits = limits;
		this.audits = audits;
		this.notifications = notifications;
		this.clock = clock;
	}

	@Transactional(noRollbackFor = EmailVerificationException.class)
	public PasswordResetAcceptedResponse request(PasswordResetRequest request, String ip) {
		if (!properties.isEnabled()) return new PasswordResetAcceptedResponse(true);
		String email = normalize(request.email());
		limits.check("PASSWORD_RESET_REQUEST_IP", ip, 5, Duration.ofMinutes(1));
		limits.check("PASSWORD_RESET_REQUEST_EMAIL", email, 3, Duration.ofHours(1));
		Account account = accounts.findByEmailIgnoreCaseAndStatus(email, AccountStatus.ACTIVE).orElse(null);
		if (account == null) return new PasswordResetAcceptedResponse(true);
		account = accounts.findByIdForUpdate(account.getId())
			.filter(value -> value.getStatus() == AccountStatus.ACTIVE)
			.orElse(null);
		if (account == null) return new PasswordResetAcceptedResponse(true);

		Instant now = clock.instant();
		var active = db.query("""
			select id,status,generation,resend_available_at from password_reset_requests
			where account_id=? and status in ('PENDING','VERIFIED') for update
			""", (rs, n) -> new ActiveRequest(rs.getObject(1, UUID.class), rs.getString(2), rs.getInt(3),
			rs.getTimestamp(4).toInstant()), account.getId()).stream().findFirst().orElse(null);
		if (active != null && "PENDING".equals(active.status()) && active.resendAt().isAfter(now))
			return new PasswordResetAcceptedResponse(true);

		UUID requestId;
		int generation;
		if (active == null) {
			requestId = UUID.randomUUID();
			generation = 1;
		}
		else if ("PENDING".equals(active.status())) {
			requestId = active.id();
			generation = active.generation() + 1;
			cancelOtpOutbox(requestId, now);
		}
		else {
			db.update("update password_reset_requests set status='SUPERSEDED',updated_at=?,version=version+1 where id=?",
				Timestamp.from(now), active.id());
			db.update("update password_reset_sessions set revoked_at=?,updated_at=? where request_id=? and used_at is null and revoked_at is null",
				Timestamp.from(now), Timestamp.from(now), active.id());
			requestId = UUID.randomUUID();
			generation = 1;
		}

		String otp = crypto.otp();
		String hmacVersion = properties.getHmacActiveVersion();
		Instant expiresAt = now.plus(properties.getOtpTtl());
		Instant resendAt = now.plus(properties.getResendCooldown());
		String digest = crypto.otpDigest(boundOtp(requestId, generation, otp), hmacVersion);
		if (active == null || "VERIFIED".equals(active.status())) {
			db.update("""
				insert into password_reset_requests(id,account_id,status,generation,otp_digest,hmac_key_version,
				 expires_at,max_attempts,resend_available_at) values (?,?,'PENDING',?,?,?,?,?,?)
				""", requestId, account.getId(), generation, digest, hmacVersion, Timestamp.from(expiresAt),
				properties.getMaxAttempts(), Timestamp.from(resendAt));
			audits.save(new AuditEvent(UUID.randomUUID(), "PASSWORD_RESET_REQUESTED", "ACCOUNT", account.getId(),
				Map.of("requestId", requestId.toString())));
		}
		else {
			db.update("""
				update password_reset_requests set generation=?,otp_digest=?,hmac_key_version=?,expires_at=?,
				 attempt_count=0,max_attempts=?,resend_available_at=?,updated_at=?,version=version+1 where id=?
				""", generation, digest, hmacVersion, Timestamp.from(expiresAt), properties.getMaxAttempts(),
				Timestamp.from(resendAt), Timestamp.from(now), requestId);
			audits.save(new AuditEvent(UUID.randomUUID(), "PASSWORD_RESET_OTP_RESENT", "ACCOUNT", account.getId(),
				Map.of("requestId", requestId.toString(), "generation", generation)));
		}
		enqueueOtp(account, requestId, generation, otp, expiresAt, now);
		return new PasswordResetAcceptedResponse(true);
	}

	@Transactional(noRollbackFor = PasswordResetException.class)
	public VerifyResult verify(PasswordResetVerifyRequest request, String ip) {
		if (!properties.isEnabled()) invalidOtp();
		String email = normalize(request.email());
		limits.check("PASSWORD_RESET_VERIFY_IP", ip, 5, Duration.ofMinutes(1));
		Instant now = clock.instant();
		var rows = db.query("""
			select r.id,r.account_id,r.status,r.generation,r.otp_digest,r.hmac_key_version,r.expires_at,
			 r.attempt_count,r.max_attempts from password_reset_requests r join accounts a on a.id=r.account_id
			where lower(a.email)=lower(?) and a.status='ACTIVE' and r.status='PENDING'
			order by r.created_at desc limit 1 for update of r,a
			""", this::requestRow, email);
		if (rows.isEmpty()) invalidOtp();
		RequestRow row = rows.getFirst();
		boolean matches = crypto.matchesOtp(boundOtp(row.id(), row.generation(), request.otp()),
			row.digest(), row.hmacVersion());
		if (!row.expiresAt().isAfter(now) || row.attempts() >= row.maxAttempts()) {
			expire(row, now);
			invalidOtp();
		}
		if (!matches) {
			int attempts = row.attempts() + 1;
			String status = attempts >= row.maxAttempts() ? "EXPIRED" : "PENDING";
			db.update("update password_reset_requests set attempt_count=?,status=?,updated_at=?,version=version+1 where id=?",
				attempts, status, Timestamp.from(now), row.id());
			if ("EXPIRED".equals(status)) {
				cancelOtpOutbox(row.id(), now);
				audits.save(new AuditEvent(UUID.randomUUID(), "PASSWORD_RESET_EXPIRED", "ACCOUNT", row.accountId(),
					Map.of("requestId", row.id().toString(), "reason", "ATTEMPT_LIMIT")));
			}
			invalidOtp();
		}

		String rawToken = crypto.token();
		Instant sessionExpiresAt = now.plus(SESSION_TTL);
		db.update("update password_reset_requests set status='VERIFIED',verified_at=?,updated_at=?,version=version+1 where id=?",
			Timestamp.from(now), Timestamp.from(now), row.id());
		cancelOtpOutbox(row.id(), now);
		db.update("update password_reset_sessions set revoked_at=?,updated_at=? where account_id=? and used_at is null and revoked_at is null",
			Timestamp.from(now), Timestamp.from(now), row.accountId());
		db.update("insert into password_reset_sessions(id,request_id,account_id,token_hash,expires_at) values (?,?,?,?,?)",
			UUID.randomUUID(), row.id(), row.accountId(), crypto.sha256(rawToken), Timestamp.from(sessionExpiresAt));
		audits.save(new AuditEvent(UUID.randomUUID(), "PASSWORD_RESET_OTP_VERIFIED", "ACCOUNT", row.accountId(),
			Map.of("requestId", row.id().toString())));
		return new VerifyResult(new PasswordResetVerifiedResponse(true, sessionExpiresAt), rawToken);
	}

	@Transactional
	public void confirm(PasswordResetConfirmRequest request, String rawSession) {
		request.validate();
		if (rawSession == null || rawSession.isBlank()) invalidSession();
		Instant now = clock.instant();
		var rows = db.query("""
			select s.id,s.request_id,s.account_id,s.expires_at,s.used_at,s.revoked_at,r.status
			from password_reset_sessions s join password_reset_requests r on r.id=s.request_id
			where s.token_hash=? for update of s,r
			""", this::sessionRow, crypto.sha256(rawSession));
		if (rows.isEmpty()) invalidSession();
		SessionRow session = rows.getFirst();
		if (session.usedAt() != null || session.revokedAt() != null || !session.expiresAt().isAfter(now)
				|| !"VERIFIED".equals(session.requestStatus())) invalidSession();
		Account account = accounts.findByIdForUpdate(session.accountId())
			.filter(value -> value.getStatus() == AccountStatus.ACTIVE)
			.orElseThrow(this::invalidSessionException);
		if (passwords.matches(request.newPassword(), account.getPasswordHash()))
			throw new PasswordResetException(HttpStatus.BAD_REQUEST, "NEW_PASSWORD_SAME_AS_CURRENT",
				"The new password must differ from the current password.");

		account.changePassword(passwords.encode(request.newPassword()));
		refreshTokens.revokeAll(account.getId());
		db.update("update password_reset_sessions set used_at=?,updated_at=? where id=?",
			Timestamp.from(now), Timestamp.from(now), session.id());
		db.update("update password_reset_requests set status='COMPLETED',completed_at=?,updated_at=?,version=version+1 where id=?",
			Timestamp.from(now), Timestamp.from(now), session.requestId());
		db.update("update password_reset_sessions set revoked_at=?,updated_at=? where account_id=? and id<>? and used_at is null and revoked_at is null",
			Timestamp.from(now), Timestamp.from(now), account.getId(), session.id());
		audits.save(new AuditEvent(UUID.randomUUID(), "PASSWORD_RESET_COMPLETED", "ACCOUNT", account.getId(),
			Map.of("requestId", session.requestId().toString())));
		notifications.write("PASSWORD_RESET_COMPLETED:" + session.requestId(), account.getId(),
			"PASSWORD_RESET_COMPLETED", "ACCOUNT", account.getId(), Map.of("securityEvent", true));
	}

	private void enqueueOtp(Account account, UUID requestId, int generation, String otp, Instant expiresAt, Instant now) {
		String encryptionVersion = properties.getEncryptionActiveVersion();
		var encrypted = crypto.encryptOtp(otp, encryptionVersion);
		db.update("""
			insert into email_outbox(id,event_key,account_id,generation,recipient_email,template_key,deliver_before,
			 payload_ciphertext,payload_nonce,encryption_key_version,status,next_retry_at)
			values (?,?,?,?,?,?,?,?,?,?,'PENDING',?)
			""", UUID.randomUUID(), "PASSWORD_RESET_OTP:" + requestId + ":" + generation, account.getId(), generation,
			account.getEmail(), TEMPLATE_KEY, Timestamp.from(expiresAt), encrypted.ciphertext(), encrypted.nonce(),
			encryptionVersion, Timestamp.from(now));
	}

	private void cancelOtpOutbox(UUID requestId, Instant now) {
		db.update("""
			update email_outbox set status='CANCELLED',payload_ciphertext=null,payload_nonce=null,locked_at=null,
			 locked_by=null,updated_at=? where event_key like ? and status in ('PENDING','FAILED','PROCESSING')
			""", Timestamp.from(now), "PASSWORD_RESET_OTP:" + requestId + ":%");
	}

	private void expire(RequestRow row, Instant now) {
		db.update("update password_reset_requests set status='EXPIRED',updated_at=?,version=version+1 where id=? and status='PENDING'",
			Timestamp.from(now), row.id());
		cancelOtpOutbox(row.id(), now);
		audits.save(new AuditEvent(UUID.randomUUID(), "PASSWORD_RESET_EXPIRED", "ACCOUNT", row.accountId(),
			Map.of("requestId", row.id().toString(), "reason", "OTP_EXPIRED")));
	}

	private String boundOtp(UUID requestId, int generation, String otp) {
		return "PASSWORD_RESET:" + requestId + ":" + generation + ":" + otp;
	}

	private String normalize(String email) { return email.trim().toLowerCase(Locale.ROOT); }
	private void invalidOtp() { throw new PasswordResetException(HttpStatus.BAD_REQUEST,
		"INVALID_OR_EXPIRED_OTP", "The OTP is invalid or expired."); }
	private void invalidSession() { throw invalidSessionException(); }
	private PasswordResetException invalidSessionException() { return new PasswordResetException(HttpStatus.UNAUTHORIZED,
		"INVALID_PASSWORD_RESET_SESSION", "The password reset session is invalid or expired."); }
	private RequestRow requestRow(ResultSet rs, int n) throws SQLException {
		return new RequestRow(rs.getObject(1, UUID.class), rs.getObject(2, UUID.class), rs.getString(3), rs.getInt(4),
			rs.getString(5), rs.getString(6), rs.getTimestamp(7).toInstant(), rs.getInt(8), rs.getInt(9));
	}
	private SessionRow sessionRow(ResultSet rs, int n) throws SQLException {
		return new SessionRow(rs.getObject(1, UUID.class), rs.getObject(2, UUID.class), rs.getObject(3, UUID.class),
			rs.getTimestamp(4).toInstant(), instant(rs, 5), instant(rs, 6), rs.getString(7));
	}
	private Instant instant(ResultSet rs, int index) throws SQLException {
		Timestamp value = rs.getTimestamp(index); return value == null ? null : value.toInstant();
	}

	public record VerifyResult(PasswordResetVerifiedResponse response, String sessionToken) { }
	private record ActiveRequest(UUID id, String status, int generation, Instant resendAt) { }
	private record RequestRow(UUID id, UUID accountId, String status, int generation, String digest,
		String hmacVersion, Instant expiresAt, int attempts, int maxAttempts) { }
	private record SessionRow(UUID id, UUID requestId, UUID accountId, Instant expiresAt, Instant usedAt,
		Instant revokedAt, String requestStatus) { }
}
