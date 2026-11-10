package com.scms.backend.emailverification.verification;

import com.scms.backend.account.AccountRepository;
import com.scms.backend.audit.AuditEvent;
import com.scms.backend.audit.AuditEventRepository;
import com.scms.backend.emailverification.DatabaseRateLimiter;
import com.scms.backend.emailverification.EmailVerificationCrypto;
import com.scms.backend.emailverification.EmailVerificationException;
import com.scms.backend.emailverification.EmailVerificationProperties;
import com.scms.backend.emailverification.PendingRegistrationException;
import com.scms.backend.emailverification.delivery.EmailMessageRenderer;
import com.scms.backend.emailverification.delivery.EmailOutboxEnqueuer;
import com.scms.backend.notification.NotificationWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmailVerificationService {

	private final JdbcTemplate db;

	private final EmailVerificationProperties properties;

	private final EmailVerificationCrypto crypto;

	private final DatabaseRateLimiter limits;

	private final Clock clock;

	private final AuditEventRepository audits;

	private final NotificationWriter notifications;

	private final AccountRepository accounts;

	private final EmailOutboxEnqueuer outbox;

	EmailVerificationService(JdbcTemplate db, EmailVerificationProperties properties, EmailVerificationCrypto crypto, DatabaseRateLimiter limits, Clock clock, AuditEventRepository audits, NotificationWriter notifications, AccountRepository accounts, EmailOutboxEnqueuer outbox) {
		this.db = db;
		this.properties = properties;
		this.crypto = crypto;
		this.limits = limits;
		this.clock = clock;
		this.audits = audits;
		this.notifications = notifications;
		this.accounts = accounts;
		this.outbox = outbox;
		this.emailVerificationRepository = new EmailVerificationRepository(this.db);
	}

	public boolean enabled() {
		return properties.isEnabled();
	}

	public Instant registrationExpiry() {
		return clock.instant().plus(properties.getRegistrationTtl());
	}

	public void rateLimitRegister(String ip, String email, String phone) {
		if (!enabled())
			return;
		limits.check("REGISTER_IP", ip, 10, Duration.ofMinutes(15));
		limits.check("REGISTER_EMAIL", normalizeEmail(email), 3, Duration.ofHours(1));
		limits.check("REGISTER_PHONE", phone.trim(), 3, Duration.ofHours(1));
	}

	@Transactional
	public EmailVerificationRegistration begin(UUID accountId, String email, Instant registrationExpiresAt) {
		Instant now = clock.instant();
		Generated generated = generate(accountId, normalizeEmail(email), 1, now, registrationExpiresAt, "EMAIL_VERIFICATION_REQUESTED");
		String session = crypto.token(), csrf = crypto.token();
		emailVerificationRepository.beginInsertEmailVerificationSessions(UUID.randomUUID(), accountId, crypto.sha256(session), crypto.sha256(csrf), Timestamp.from(registrationExpiresAt));
		return new EmailVerificationRegistration(session, csrf, generated.expiresAt(), generated.resendAt(), registrationExpiresAt, now);
	}

	public PendingRegistrationException pendingConflict(String email) {
		Instant now = clock.instant();
		var rows = emailVerificationRepository.pendingConflictFindAccounts(normalizeEmail(email), Timestamp.from(now));
		return rows.isEmpty() ? null : new PendingRegistrationException(rows.getFirst().otp(), rows.getFirst().resend(), rows.getFirst().registration(), now);
	}

	@Transactional
	public VerifyResult verify(VerifyEmailRequest request, String cookie, String ip) {
		if (!enabled())
			invalidOtp();
		limits.check("VERIFY_IP", ip, 5, Duration.ofMinutes(1));
		Instant now = clock.instant();
		String email = normalizeEmail(request.email());
		var rows = emailVerificationRepository.verifyFindAccounts(email);
		if (rows.isEmpty())
			invalidOtp();
		ChallengeRow row = rows.getFirst();
		boolean matches = crypto.matchesOtp(request.otp(), row.digest(), row.keyVersion());
		if (row.consumedAt() != null) {
			if (matches && "ACTIVE".equals(row.status()) && !row.required() && row.verifiedAt() != null)
				return new VerifyResult(new VerifyEmailResponse(true, com.scms.backend.account.AccountStatus.ACTIVE), cookieMatches(cookie, row.accountId()));
			invalidOtp();
		}
		if (!row.required() || !"INACTIVE".equals(row.status()) || row.registrationExpiresAt() == null || !row.registrationExpiresAt().isAfter(now) || row.supersededAt() != null || row.lockedAt() != null || !row.expiresAt().isAfter(now) || row.attempts() >= row.maximum())
			invalidOtp();
		if (!matches) {
			int attempts = row.attempts() + 1;
			emailVerificationRepository.verifyUpdateEmailVerificationChallenges(attempts, attempts >= row.maximum() ? Timestamp.from(now) : null, Timestamp.from(now), row.challengeId());
			invalidOtp();
		}
		var account = accounts.findById(row.accountId()).orElseThrow();
		try {
			account.verifyPendingEmail(now);
		} catch (IllegalStateException ex) {
			invalidOtp();
		}
		accounts.flush();
		emailVerificationRepository.consumeChallenge(Timestamp.from(now), Timestamp.from(now), row.challengeId());
		revokeArtifacts(row.accountId(), now, row.generation());
		cancelManualRequests(row.accountId(), now, "ACCOUNT_ALREADY_ACTIVATED");
		audits.save(new AuditEvent(UUID.randomUUID(), "EMAIL_VERIFIED", "ACCOUNT", row.accountId(), Map.of("source", "SELF_REGISTRATION")));
		audits.save(new AuditEvent(UUID.randomUUID(), "ACCOUNT_ACTIVATED", "ACCOUNT", row.accountId(), Map.of("source", "EMAIL_OTP")));
		notifications.write("ACCOUNT_ACTIVATED:" + row.accountId(), row.accountId(), "ACCOUNT_ACTIVATED", "ACCOUNT", row.accountId(), Map.of("source", "EMAIL_OTP"));
		return new VerifyResult(new VerifyEmailResponse(true, com.scms.backend.account.AccountStatus.ACTIVE), cookieMatches(cookie, row.accountId()));
	}

	@Transactional
	public ResendEmailResponse resend(ResendEmailRequest request, String ip) {
		if (!enabled()) {
			Instant now = clock.instant();
			return new ResendEmailResponse(true, now.plus(properties.getResendCooldown()), now);
		}
		limits.check("RESEND_IP", ip, 5, Duration.ofMinutes(1));
		String email = normalizeEmail(request.email());
		limits.check("RESEND_EMAIL", email, 3, Duration.ofHours(1));
		Instant now = clock.instant(), generic = now.plus(properties.getResendCooldown());
		var rows = emailVerificationRepository.resendFindAccounts(email);
		if (!rows.isEmpty()) {
			ResendRow row = rows.getFirst();
			if (row.registrationExpiresAt().isAfter(now) && !row.resendAt().isAfter(now)) {
				emailVerificationRepository.resendUpdateEmailVerificationChallenges(Timestamp.from(now), Timestamp.from(now), row.challengeId());
				cancelOutbox(row.accountId(), now);
				generate(row.accountId(), email, row.generation() + 1, now, row.registrationExpiresAt(), "EMAIL_VERIFICATION_RESENT");
			}
		}
		return new ResendEmailResponse(true, generic, now);
	}

	@Transactional
	public ChangeResult changeEmail(ChangePendingEmailRequest request, String cookie, String csrf, String ip) {
		if (!enabled())
			invalidSession();
		limits.check("CHANGE_EMAIL_IP", ip, 5, Duration.ofMinutes(1));
		Instant now = clock.instant();
		if (cookie == null || cookie.isBlank() || csrf == null || csrf.isBlank())
			invalidSession();
		var sessions = emailVerificationRepository.changeEmailFindEmailVerificationSessions(crypto.sha256(cookie));
		if (sessions.isEmpty())
			invalidSession();
		SessionRow session = sessions.getFirst();
		if (!session.expiresAt().isAfter(now) || !crypto.secureHashMatch(csrf, session.csrfHash()))
			invalidSession();
		if (session.changeCount() >= 3)
			throw new EmailVerificationException(HttpStatus.CONFLICT, "EMAIL_CHANGE_LIMIT_EXCEEDED", "Email change limit exceeded.");
		String email = normalizeEmail(request.newEmail());
		try {
			int changed = emailVerificationRepository.changeEmailUpdateAccounts(email, Timestamp.from(now), session.accountId(), Timestamp.from(now));
			if (changed != 1)
				invalidSession();
		} catch (DataIntegrityViolationException ex) {
			throw new EmailVerificationException(HttpStatus.CONFLICT, "EMAIL_ALREADY_EXISTS", "Email is already in use.");
		}
		Integer generation = emailVerificationRepository.changeEmailFindEmailVerificationChallenges(session.accountId());
		emailVerificationRepository.changeEmailUpdateEmailVerificationChallenges(Timestamp.from(now), Timestamp.from(now), session.accountId());
		cancelOutbox(session.accountId(), now);
		emailVerificationRepository.changeEmailUpdateEmailVerificationSessions(Timestamp.from(now), Timestamp.from(now), session.id());
		cancelManualRequests(session.accountId(), now, "EMAIL_CHANGED");
		Generated generated = generate(session.accountId(), email, (generation == null ? 0 : generation) + 1, now, session.registrationExpiresAt(), "EMAIL_VERIFICATION_EMAIL_CHANGED");
		String newToken = crypto.token(), newCsrf = crypto.token();
		emailVerificationRepository.changeEmailInsertEmailVerificationSessions(UUID.randomUUID(), session.accountId(), crypto.sha256(newToken), crypto.sha256(newCsrf), Timestamp.from(session.registrationExpiresAt()), session.changeCount() + 1);
		return new ChangeResult(new ChangePendingEmailResponse(true, mask(email), newCsrf, generated.expiresAt(), generated.resendAt(), session.registrationExpiresAt(), now), newToken);
	}

	private Generated generate(UUID accountId, String email, int generation, Instant now, Instant registrationExpiresAt, String auditAction) {
		String otp = crypto.otp();
		Instant expires = min(now.plus(properties.getOtpTtl()), registrationExpiresAt);
		Instant resend = now.plus(properties.getResendCooldown());
		String hmacVersion = properties.getHmacActiveVersion();
		UUID challengeId = UUID.randomUUID();
		emailVerificationRepository.generateInsertEmailVerificationChallenges(challengeId, accountId, generation, crypto.otpDigest(otp, hmacVersion), hmacVersion, Timestamp.from(expires), properties.getMaxAttempts(), Timestamp.from(resend));
		outbox.enqueue("EMAIL_VERIFICATION_OTP:" + accountId + ":" + generation, accountId, generation, email, EmailMessageRenderer.VERIFICATION, otp, expires, now);
		audits.save(new AuditEvent(UUID.randomUUID(), auditAction, "ACCOUNT", accountId, Map.of("source", "SELF_REGISTRATION", "generation", generation)));
		return new Generated(expires, resend);
	}

	private void revokeArtifacts(UUID accountId, Instant now, int generation) {
		emailVerificationRepository.revokeArtifactsUpdateEmailVerificationSessions(Timestamp.from(now), Timestamp.from(now), accountId);
		emailVerificationRepository.revokeArtifactsUpdateEmailOutbox(Timestamp.from(now), accountId, generation);
	}

	private void cancelOutbox(UUID accountId, Instant now) {
		emailVerificationRepository.cancelOutboxUpdateEmailOutbox(Timestamp.from(now), accountId);
	}

	private void cancelManualRequests(UUID accountId, Instant now, String terminalReason) {
		var requests = emailVerificationRepository.cancelManualRequestsFindEmailVerificationManualActivationRequests(accountId);
		emailVerificationRepository.cancelManualRequestsUpdateEmailVerificationManualActivationRequests(terminalReason, Timestamp.from(now), accountId);
		for (var request : requests) audits.save(new AuditEvent(UUID.randomUUID(), "MANUAL_ACTIVATION_REQUEST_CANCELLED", "MANUAL_ACTIVATION_REQUEST", request.id(), Map.of("accountId", accountId.toString(), "requestedByAccountId", request.requestedBy().toString(), "reasonCode", request.reasonCode(), "verificationMethod", request.verificationMethod(), "terminalReason", terminalReason, "source", "SELF_REGISTRATION")));
	}

	private boolean cookieMatches(String cookie, UUID accountId) {
		if (cookie == null || cookie.isBlank())
			return false;
		Integer count = emailVerificationRepository.cookieMatchesFindEmailVerificationSessions(crypto.sha256(cookie), accountId);
		return count != null && count > 0;
	}

	private void invalidOtp() {
		throw new EmailVerificationException(HttpStatus.BAD_REQUEST, "INVALID_OR_EXPIRED_OTP", "The OTP is invalid or expired.");
	}

	private void invalidSession() {
		throw new EmailVerificationException(HttpStatus.UNAUTHORIZED, "INVALID_PENDING_SESSION", "The pending verification session is invalid or expired.");
	}

	private String normalizeEmail(String email) {
		return email.trim().toLowerCase(java.util.Locale.ROOT);
	}

	public static String mask(String email) {
		int at = email.indexOf('@');
		return at < 1 ? "***" : email.substring(0, 1) + "***" + email.substring(at);
	}

	private Instant min(Instant a, Instant b) {
		return a.isBefore(b) ? a : b;
	}

	private Instant instant(ResultSet rs, int index) throws SQLException {
		Timestamp value = rs.getTimestamp(index);
		return value == null ? null : value.toInstant();
	}

	public record VerifyResult(VerifyEmailResponse response, boolean clearCookie) {
	}

	public record ChangeResult(ChangePendingEmailResponse response, String sessionToken) {
	}

	private record Generated(Instant expiresAt, Instant resendAt) {
	}

	record PendingTimes(Instant otp, Instant resend, Instant registration) {
	}

	record ResendRow(UUID accountId, Instant registrationExpiresAt, UUID challengeId, int generation, Instant resendAt) {
	}

	record SessionRow(UUID id, UUID accountId, String csrfHash, Instant expiresAt, int changeCount, Instant registrationExpiresAt) {
	}

	record ManualRequestAudit(UUID id, UUID requestedBy, String reasonCode, String verificationMethod) {
	}

	record ChallengeRow(UUID accountId, String status, boolean required, Instant registrationExpiresAt, Instant verifiedAt, UUID challengeId, String digest, String keyVersion, Instant expiresAt, int attempts, int maximum, Instant consumedAt, Instant supersededAt, Instant lockedAt, int generation) {
	}

	private final EmailVerificationRepository emailVerificationRepository;
}
