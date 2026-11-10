package com.scms.backend.emailverification.emailchange;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.audit.AuditEvent;
import com.scms.backend.audit.AuditEventRepository;
import com.scms.backend.auth.RefreshTokenService;
import com.scms.backend.emailverification.AccountEmailChangeStatus;
import com.scms.backend.emailverification.DatabaseRateLimiter;
import com.scms.backend.emailverification.EmailVerificationCrypto;
import com.scms.backend.emailverification.EmailVerificationException;
import com.scms.backend.emailverification.EmailVerificationProperties;
import com.scms.backend.emailverification.delivery.EmailOutboxEnqueuer;
import com.scms.backend.emailverification.verification.EmailVerificationService;
import com.scms.backend.notification.NotificationWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountEmailChangeService {

	private static final String TEMPLATE_KEY = "ACCOUNT_EMAIL_CHANGE_OTP";

	private static final String EVENT_PREFIX = "ACCOUNT_EMAIL_CHANGE_OTP:";

	private final JdbcTemplate db;

	private final AccountRepository accounts;

	private final PasswordEncoder passwords;

	private final EmailVerificationProperties properties;

	private final EmailVerificationCrypto crypto;

	private final DatabaseRateLimiter limits;

	private final AuditEventRepository audits;

	private final NotificationWriter notifications;

	private final RefreshTokenService refreshTokens;

	private final Clock clock;

	private final EmailOutboxEnqueuer outbox;

	AccountEmailChangeService(JdbcTemplate db, AccountRepository accounts, PasswordEncoder passwords, EmailVerificationProperties properties, EmailVerificationCrypto crypto, DatabaseRateLimiter limits, AuditEventRepository audits, NotificationWriter notifications, RefreshTokenService refreshTokens, Clock clock, EmailOutboxEnqueuer outbox) {
		this.db = db;
		this.accounts = accounts;
		this.passwords = passwords;
		this.properties = properties;
		this.crypto = crypto;
		this.limits = limits;
		this.audits = audits;
		this.notifications = notifications;
		this.refreshTokens = refreshTokens;
		this.clock = clock;
		this.outbox = outbox;
		this.accountEmailChangeRepository = new AccountEmailChangeRepository(this.db);
	}

	@Transactional(noRollbackFor = EmailVerificationException.class)
	public AccountEmailChangeResponse begin(UUID accountId, AccountEmailChangeRequest request, String ip) {
		return create(accountId, request, ip, false);
	}

	@Transactional(noRollbackFor = EmailVerificationException.class)
	public AccountEmailChangeResponse replace(UUID accountId, AccountEmailChangeRequest request, String ip) {
		return create(accountId, request, ip, true);
	}

	private AccountEmailChangeResponse create(UUID accountId, AccountEmailChangeRequest request, String ip, boolean replacePending) {
		ensureEnabled();
		limits.check("ACCOUNT_EMAIL_CHANGE_ACCOUNT", accountId.toString(), 5, Duration.ofMinutes(15));
		limits.check("ACCOUNT_EMAIL_CHANGE_IP", ip, 10, Duration.ofMinutes(15));
		String email = normalize(request.newEmail());
		limits.check("ACCOUNT_EMAIL_CHANGE_TARGET", email, 5, Duration.ofHours(1));
		Account account = activeForUpdate(accountId);
		if (!passwords.matches(request.currentPassword(), account.getPasswordHash()))
			throw error(HttpStatus.BAD_REQUEST, "CURRENT_PASSWORD_INCORRECT", "The current password is incorrect.");
		if (email.equalsIgnoreCase(account.getEmail()))
			throw error(HttpStatus.CONFLICT, "EMAIL_UNCHANGED", "The new email must differ from the current email.");
		if (accounts.existsByOccupiedEmail(email))
			throw emailExists();
		Instant now = clock.instant();
		List<Row> existing = pendingForUpdate(accountId);
		if (!existing.isEmpty() && !replacePending)
			throw error(HttpStatus.CONFLICT, "EMAIL_CHANGE_PENDING", "An email change is already pending.");
		if (!existing.isEmpty())
			supersede(existing.getFirst(), now);
		UUID id = UUID.randomUUID();
		int generation = 1;
		String otp = crypto.otp();
		String hmacVersion = properties.getHmacActiveVersion();
		Instant expires = now.plus(properties.getOtpTtl());
		Instant resend = now.plus(properties.getResendCooldown());
		accountEmailChangeRepository.createInsertStaffEmailChangeRequests(id, accountId, email, generation, digest(id, generation, otp, hmacVersion), hmacVersion, Timestamp.from(expires), Timestamp.from(resend), properties.getMaxAttempts(), Timestamp.from(now), Timestamp.from(now));
		enqueue(id, accountId, email, otp, generation, expires, now);
		audits.save(new AuditEvent(UUID.randomUUID(), accountId, "ACCOUNT_EMAIL_CHANGE_REQUESTED", "ACCOUNT", accountId, Map.of("requestId", id.toString(), "pendingEmail", EmailVerificationService.mask(email))));
		notifications.write("ACCOUNT_EMAIL_CHANGE_REQUESTED:" + id, accountId, "ACCOUNT_EMAIL_CHANGE_REQUESTED", "ACCOUNT", accountId, Map.of("requestId", id.toString(), "pendingEmail", EmailVerificationService.mask(email), "securityEvent", true));
		return response(email, expires, resend, now);
	}

	@Transactional(noRollbackFor = EmailVerificationException.class)
	public AccountEmailChangeVerifyResponse verify(UUID accountId, AccountEmailChangeVerifyRequest request) {
		ensureEnabled();
		limits.check("ACCOUNT_EMAIL_CHANGE_VERIFY", accountId.toString(), 5, Duration.ofMinutes(1));
		Instant now = clock.instant();
		List<Row> rows = pendingForUpdate(accountId);
		if (rows.isEmpty()) {
			List<String> completed = accountEmailChangeRepository.verifyFindStaffEmailChangeRequests(accountId);
			if (!completed.isEmpty())
				return new AccountEmailChangeVerifyResponse(true, completed.getFirst());
			throw invalidOtp();
		}
		Row row = rows.getFirst();
		if (!row.expiresAt().isAfter(now) || row.attempts() >= row.maximum()) {
			expire(row, now, "OTP_EXPIRED");
			throw invalidOtp();
		}
		String candidate = row.purposeBound() ? bound(row.id(), row.generation(), request.otp()) : request.otp();
		if (!crypto.matchesOtp(candidate, row.digest(), row.keyVersion())) {
			int attempts = row.attempts() + 1;
			String status = attempts >= row.maximum() ? "EXPIRED" : "PENDING";
			accountEmailChangeRepository.verifyUpdateStaffEmailChangeRequests(attempts, status, Timestamp.from(now), row.id());
			if ("EXPIRED".equals(status)) {
				cancelOutbox(row.id(), now);
				audits.save(new AuditEvent(UUID.randomUUID(), "ACCOUNT_EMAIL_CHANGE_EXPIRED", "ACCOUNT", row.accountId(), Map.of("requestId", row.id().toString(), "reason", "ATTEMPT_LIMIT")));
			}
			throw invalidOtp();
		}
		Account account = activeForUpdate(accountId);
		lockEmailClaim(row.email());
		if (accounts.existsByOccupiedEmail(row.email())) {
			expire(row, now, "EMAIL_CLAIMED");
			throw emailExists();
		}
		account.changeVerifiedEmail(row.email(), now);
		accounts.flush();
		accountEmailChangeRepository.markEmailChangeCompleted(Timestamp.from(now), Timestamp.from(now), Timestamp.from(now), row.id());
		cancelOutbox(row.id(), now);
		refreshTokens.revokeAll(accountId);
		audits.save(new AuditEvent(UUID.randomUUID(), accountId, "ACCOUNT_EMAIL_CHANGE_COMPLETED", "ACCOUNT", accountId, Map.of("requestId", row.id().toString(), "oldEmail", EmailVerificationService.mask(row.currentEmail()), "newEmail", EmailVerificationService.mask(row.email()))));
		notifications.write("ACCOUNT_EMAIL_CHANGE_COMPLETED:" + row.id(), accountId, "ACCOUNT_EMAIL_CHANGE_COMPLETED", "ACCOUNT", accountId, Map.of("requestId", row.id().toString(), "newEmail", EmailVerificationService.mask(row.email()), "securityEvent", true));
		return new AccountEmailChangeVerifyResponse(true, row.email());
	}

	@Transactional(noRollbackFor = EmailVerificationException.class)
	public AccountEmailChangeResponse resend(UUID accountId) {
		ensureEnabled();
		limits.check("ACCOUNT_EMAIL_CHANGE_RESEND", accountId.toString(), 3, Duration.ofHours(1));
		Instant now = clock.instant();
		List<Row> rows = pendingForUpdate(accountId);
		if (rows.isEmpty())
			throw invalidOtp();
		Row row = rows.getFirst();
		if (!row.expiresAt().isAfter(now)) {
			expire(row, now, "OTP_EXPIRED");
			throw invalidOtp();
		}
		if (row.resendAt().isAfter(now))
			throw new EmailVerificationException(HttpStatus.TOO_MANY_REQUESTS, "RESEND_COOLDOWN", "Wait before requesting another OTP.", Math.max(1, row.resendAt().getEpochSecond() - now.getEpochSecond()));
		String otp = crypto.otp();
		int generation = row.generation() + 1;
		String hmacVersion = properties.getHmacActiveVersion();
		Instant expires = now.plus(properties.getOtpTtl());
		Instant resend = now.plus(properties.getResendCooldown());
		accountEmailChangeRepository.resendUpdateStaffEmailChangeRequests(generation, digest(row.id(), generation, otp, hmacVersion), hmacVersion, Timestamp.from(expires), Timestamp.from(resend), Timestamp.from(now), row.id());
		cancelOutbox(row.id(), now);
		enqueue(row.id(), accountId, row.email(), otp, generation, expires, now);
		audits.save(new AuditEvent(UUID.randomUUID(), accountId, "ACCOUNT_EMAIL_CHANGE_RESENT", "ACCOUNT", accountId, Map.of("requestId", row.id().toString(), "generation", generation, "pendingEmail", EmailVerificationService.mask(row.email()))));
		return response(row.email(), expires, resend, now);
	}

	@Transactional
	public void cancel(UUID accountId) {
		Instant now = clock.instant();
		List<Row> rows = pendingForUpdate(accountId);
		if (rows.isEmpty())
			return;
		Row row = rows.getFirst();
		accountEmailChangeRepository.cancelUpdateStaffEmailChangeRequests(Timestamp.from(now), Timestamp.from(now), row.id());
		cancelOutbox(row.id(), now);
		audits.save(new AuditEvent(UUID.randomUUID(), accountId, "ACCOUNT_EMAIL_CHANGE_CANCELLED", "ACCOUNT", accountId, Map.of("requestId", row.id().toString(), "pendingEmail", EmailVerificationService.mask(row.email()))));
	}

	@Transactional(readOnly = true)
	public AccountEmailChangeStatus status(UUID accountId) {
		active(accountId);
		Instant now = clock.instant();
		List<AccountEmailChangeStatus> rows = accountEmailChangeRepository.statusFindStaffEmailChangeRequests(accountId, Timestamp.from(now));
		return rows.isEmpty() ? AccountEmailChangeStatus.none() : rows.getFirst();
	}

	@Scheduled(cron = "${scms.email-verification.cleanup-cron:0 0 * * * *}")
	@Transactional
	public void expirePending() {
		Instant now = clock.instant();
		List<Expired> rows = accountEmailChangeRepository.expirePendingFindStaffEmailChangeRequests(Timestamp.from(now), Timestamp.from(now));
		for (Expired row : rows) {
			cancelOutbox(row.id(), now);
			audits.save(new AuditEvent(UUID.randomUUID(), "ACCOUNT_EMAIL_CHANGE_EXPIRED", "ACCOUNT", row.accountId(), Map.of("requestId", row.id().toString(), "pendingEmail", EmailVerificationService.mask(row.email()), "reason", "OTP_EXPIRED")));
		}
	}

	private void supersede(Row row, Instant now) {
		accountEmailChangeRepository.supersedeUpdateStaffEmailChangeRequests(Timestamp.from(now), Timestamp.from(now), row.id());
		cancelOutbox(row.id(), now);
		audits.save(new AuditEvent(UUID.randomUUID(), row.accountId(), "ACCOUNT_EMAIL_CHANGE_SUPERSEDED", "ACCOUNT", row.accountId(), Map.of("requestId", row.id().toString(), "pendingEmail", EmailVerificationService.mask(row.email()))));
	}

	private void expire(Row row, Instant now, String reason) {
		accountEmailChangeRepository.expireUpdateStaffEmailChangeRequests(Timestamp.from(now), row.id());
		cancelOutbox(row.id(), now);
		audits.save(new AuditEvent(UUID.randomUUID(), "ACCOUNT_EMAIL_CHANGE_EXPIRED", "ACCOUNT", row.accountId(), Map.of("requestId", row.id().toString(), "pendingEmail", EmailVerificationService.mask(row.email()), "reason", reason)));
	}

	private void enqueue(UUID requestId, UUID accountId, String email, String otp, int generation, Instant expires, Instant now) {
		outbox.enqueue(EVENT_PREFIX + requestId + ":" + generation, accountId, generation, email, TEMPLATE_KEY, otp, expires, now);
	}

	private void cancelOutbox(UUID requestId, Instant now) {
		accountEmailChangeRepository.cancelOutboxUpdateEmailOutbox(Timestamp.from(now), EVENT_PREFIX + requestId + ":%", "STAFF_EMAIL_CHANGE_OTP:" + requestId + ":%");
	}

	private List<Row> pendingForUpdate(UUID accountId) {
		return accountEmailChangeRepository.pendingForUpdateFindStaffEmailChangeRequests(accountId);
	}

	private Account activeForUpdate(UUID id) {
		return accounts.findByIdForUpdate(id).filter(account -> account.getStatus() == AccountStatus.ACTIVE).orElseThrow(() -> error(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "The authenticated account no longer exists or is not active."));
	}

	private Account active(UUID id) {
		return accounts.findById(id).filter(account -> account.getStatus() == AccountStatus.ACTIVE).orElseThrow(() -> error(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "The authenticated account no longer exists or is not active."));
	}

	private void lockEmailClaim(String email) {
		accountEmailChangeRepository.lockEmailClaimFindRows(email);
	}

	private String digest(UUID requestId, int generation, String otp, String version) {
		return crypto.otpDigest(bound(requestId, generation, otp), version);
	}

	private String bound(UUID requestId, int generation, String otp) {
		return "ACCOUNT_EMAIL_CHANGE:" + requestId + ":" + generation + ":" + otp;
	}

	private AccountEmailChangeResponse response(String email, Instant expires, Instant resend, Instant now) {
		return new AccountEmailChangeResponse(true, EmailVerificationService.mask(email), expires, resend, now);
	}

	private Instant instant(ResultSet rs, int index) throws SQLException {
		return rs.getTimestamp(index).toInstant();
	}

	private String normalize(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}

	private void ensureEnabled() {
		if (!properties.isEnabled())
			throw error(HttpStatus.SERVICE_UNAVAILABLE, "EMAIL_VERIFICATION_UNAVAILABLE", "Email verification is not configured.");
	}

	private EmailVerificationException invalidOtp() {
		return error(HttpStatus.BAD_REQUEST, "INVALID_OR_EXPIRED_OTP", "The OTP is invalid or expired.");
	}

	private EmailVerificationException emailExists() {
		return error(HttpStatus.CONFLICT, "EMAIL_ALREADY_EXISTS", "Email is already in use.");
	}

	private EmailVerificationException error(HttpStatus status, String code, String message) {
		return new EmailVerificationException(status, code, message);
	}

	record Row(UUID id, UUID accountId, String email, int generation, String digest, String keyVersion, Instant expiresAt, Instant resendAt, int attempts, int maximum, String currentEmail, boolean purposeBound) {
	}

	record Expired(UUID id, UUID accountId, String email) {
	}

	private final AccountEmailChangeRepository accountEmailChangeRepository;
}
