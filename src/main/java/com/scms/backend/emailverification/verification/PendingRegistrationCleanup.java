package com.scms.backend.emailverification.verification;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.audit.AuditEvent;
import com.scms.backend.audit.AuditEventRepository;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class PendingRegistrationCleanup {

	private final JdbcTemplate db;

	private final AccountRepository accounts;

	private final AuditEventRepository audits;

	private final Clock clock;

	PendingRegistrationCleanup(JdbcTemplate db, AccountRepository accounts, AuditEventRepository audits, Clock clock) {
		this.db = db;
		this.accounts = accounts;
		this.audits = audits;
		this.clock = clock;
		this.pendingRegistrationCleanupRepository = new PendingRegistrationCleanupRepository(this.db);
	}

	@Scheduled(cron = "${scms.email-verification.cleanup-cron:0 0 * * * *}")
	@Transactional
	void cleanup() {
		Instant now = clock.instant();
		List<UUID> ids = pendingRegistrationCleanupRepository.cleanupFindAccounts(Timestamp.from(now));
		for (UUID id : ids) {
			Account account = accounts.findById(id).orElseThrow();
			account.expirePendingRegistration(now);
			var manualRequests = pendingRegistrationCleanupRepository.cleanupFindEmailVerificationManualActivationRequests(id);
			pendingRegistrationCleanupRepository.cleanupUpdateEmailVerificationChallenges(Timestamp.from(now), Timestamp.from(now), id);
			pendingRegistrationCleanupRepository.cleanupUpdateEmailVerificationSessions(Timestamp.from(now), Timestamp.from(now), id);
			pendingRegistrationCleanupRepository.cleanupUpdateEmailOutbox(Timestamp.from(now), id);
			pendingRegistrationCleanupRepository.cleanupUpdateRefreshTokens(Timestamp.from(now), id);
			pendingRegistrationCleanupRepository.cleanupUpdateEmailVerificationManualActivationRequests(Timestamp.from(now), id);
			for (var request : manualRequests) audits.save(new AuditEvent(UUID.randomUUID(), "MANUAL_ACTIVATION_REQUEST_EXPIRED", "MANUAL_ACTIVATION_REQUEST", request.id(), Map.of("accountId", id.toString(), "requestedByAccountId", request.requestedBy().toString(), "reasonCode", request.reasonCode(), "verificationMethod", request.verificationMethod(), "terminalReason", "REGISTRATION_EXPIRED", "source", "SCHEDULED_CLEANUP")));
			audits.save(new AuditEvent(UUID.randomUUID(), "EMAIL_VERIFICATION_REGISTRATION_EXPIRED", "ACCOUNT", id, Map.of("source", "SCHEDULED_CLEANUP")));
		}
		accounts.flush();
		pendingRegistrationCleanupRepository.cleanupDeleteEmailVerificationRateLimits(Timestamp.from(now.minusSeconds(3600)));
	}

	record ManualRequestAudit(UUID id, UUID requestedBy, String reasonCode, String verificationMethod) {
	}

	private final PendingRegistrationCleanupRepository pendingRegistrationCleanupRepository;
}
