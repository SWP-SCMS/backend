package com.scms.backend.emailverification.manualactivation;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.audit.AuditEvent;
import com.scms.backend.audit.AuditEventRepository;
import com.scms.backend.auth.InvalidAuthenticatedAccountException;
import com.scms.backend.emailverification.DatabaseRateLimiter;
import com.scms.backend.emailverification.EmailVerificationException;
import com.scms.backend.emailverification.ManualActivationStatus;
import com.scms.backend.emailverification.verification.PendingVerificationAccountResponse;
import com.scms.backend.notification.NotificationWriter;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ManualActivationService {

	private final JdbcTemplate db;

	private final AccountRepository accounts;

	private final AuditEventRepository audits;

	private final NotificationWriter notifications;

	private final DatabaseRateLimiter limits;

	private final Clock clock;

	ManualActivationService(JdbcTemplate db, AccountRepository accounts, AuditEventRepository audits, NotificationWriter notifications, DatabaseRateLimiter limits, Clock clock) {
		this.db = db;
		this.accounts = accounts;
		this.audits = audits;
		this.notifications = notifications;
		this.limits = limits;
		this.clock = clock;
		this.manualActivationRepository = new ManualActivationRepository(this.db);
	}

	@Transactional(readOnly = true)
	public PendingVerificationAccountResponse search(UUID receptionist, String query) {
		ensure(receptionist, AccountRole.RECEPTIONIST);
		String q = query == null ? "" : query.trim();
		if (q.isEmpty())
			throw new EmailVerificationException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "query must not be blank.");
		Instant now = clock.instant();
		List<PendingVerificationAccountResponse> rows = manualActivationRepository.searchFindAccounts(Timestamp.from(now), q, q, q);
		if (rows.size() != 1)
			throw error(HttpStatus.NOT_FOUND, "PENDING_ACCOUNT_NOT_FOUND", "Pending account was not found.");
		return rows.getFirst();
	}

	@Transactional
	public ManualActivationResponse create(UUID receptionist, ManualActivationCreateRequest request) {
		ensure(receptionist, AccountRole.RECEPTIONIST);
		limits.check("MANUAL_CREATE_ACTOR", receptionist.toString(), 10, Duration.ofHours(1));
		Instant now = clock.instant();
		RequesterSnapshot requester = requesterSnapshot(receptionist);
		var rows = manualActivationRepository.createFindAccounts(request.memberId().trim());
		if (rows.isEmpty())
			throw error(HttpStatus.NOT_FOUND, "PENDING_ACCOUNT_NOT_FOUND", "Pending account was not found.");
		PendingAccount pending = rows.getFirst();
		Integer eligible = manualActivationRepository.countEligibleInactiveMembers(pending.id(), Timestamp.from(now));
		if (eligible == null || eligible != 1)
			throw error(HttpStatus.CONFLICT, "MANUAL_ACTIVATION_NOT_ELIGIBLE", "Account is not eligible for manual activation.");
		UUID id = UUID.randomUUID();
		try {
			manualActivationRepository.createInsertEmailVerificationManualActivationRequests(id, pending.id(), receptionist, requester.name(), requester.role().name(), request.reasonCode().name(), request.reasonDetail().trim(), request.verificationMethod().name(), request.evidenceNote().trim(), Timestamp.from(now), Timestamp.from(pending.expiresAt()));
		} catch (DataIntegrityViolationException ex) {
			throw error(HttpStatus.CONFLICT, "MANUAL_ACTIVATION_REQUEST_PENDING", "A manual activation request is already pending.");
		}
		audits.save(new AuditEvent(UUID.randomUUID(), receptionist, "MANUAL_ACTIVATION_REQUESTED", "MANUAL_ACTIVATION_REQUEST", id, Map.of("accountId", pending.id().toString(), "requestId", id.toString(), "reasonCode", request.reasonCode().name(), "verificationMethod", request.verificationMethod().name())));
		for (Account manager : accounts.findAllByRoleAndStatusOrderByIdAsc(AccountRole.MANAGER, AccountStatus.ACTIVE)) notifications.write("manual-activation:" + id + ":review-required:" + manager.getId(), manager.getId(), "MANUAL_ACTIVATION_REVIEW_REQUIRED", "MANUAL_ACTIVATION_REQUEST", id, Map.of("requestId", id.toString(), "memberCode", pending.memberCode(), "requestedByName", requester.name(), "status", "PENDING"));
		return getForActor(receptionist, id, AccountRole.RECEPTIONIST);
	}

	@Transactional(readOnly = true)
	public ManualActivationPageResponse list(UUID actor, AccountRole role, ManualActivationStatus status, int page, int size) {
		ensure(actor, role);
		validatePage(page, size);
		return activationPage(actor, role, status, page, size);
	}

	@Transactional(readOnly = true)
	public ManualActivationResponse getForActor(UUID actor, UUID id, AccountRole role) {
		ensure(actor, role);
		return activationDetail(actor, id, role);
	}

	@Transactional
	public ManualActivationResponse approve(UUID manager, UUID requestId, ManualActivationDecisionRequest request) {
		ensure(manager, AccountRole.MANAGER);
		limits.check("MANUAL_DECISION_ACTOR", manager.toString(), 30, Duration.ofMinutes(1));
		Instant now = clock.instant();
		RequestLock locked = lockRequest(requestId);
		if (locked.status() != ManualActivationStatus.PENDING)
			throw error(HttpStatus.CONFLICT, "MANUAL_ACTIVATION_REQUEST_DECIDED", "Manual activation request is no longer pending.");
		Account account = accounts.findByIdForUpdate(locked.accountId()).orElseThrow();
		if (!locked.expiresAt().isAfter(now))
			throw error(HttpStatus.CONFLICT, "MANUAL_ACTIVATION_NOT_ELIGIBLE", "Manual activation request has expired.");
		try {
			account.approveManualEmailVerificationBypass(now);
		} catch (IllegalStateException ex) {
			throw error(HttpStatus.CONFLICT, "MANUAL_ACTIVATION_NOT_ELIGIBLE", "Account is not eligible for manual activation.");
		}
		accounts.flush();
		manualActivationRepository.approveUpdateEmailVerificationManualActivationRequests(manager, request.decisionNote().trim(), Timestamp.from(now), Timestamp.from(now), requestId);
		terminateArtifacts(locked.accountId(), now);
		Map<String, Object> data = Map.of("requestId", requestId.toString(), "accountId", locked.accountId().toString(), "requestedByAccountId", locked.requestedBy().toString(), "approvedByAccountId", manager.toString(), "reasonCode", locked.reasonCode(), "verificationMethod", locked.verificationMethod());
		audits.save(new AuditEvent(UUID.randomUUID(), manager, "MANUAL_ACTIVATION_REQUEST_APPROVED", "MANUAL_ACTIVATION_REQUEST", requestId, data));
		audits.save(new AuditEvent(UUID.randomUUID(), manager, "EMAIL_VERIFICATION_BYPASSED", "ACCOUNT", locked.accountId(), data));
		audits.save(new AuditEvent(UUID.randomUUID(), manager, "ACCOUNT_ACTIVATED_MANUALLY", "ACCOUNT", locked.accountId(), data));
		notifications.write("ACCOUNT_ACTIVATED_MANUALLY:" + locked.accountId(), locked.accountId(), "ACCOUNT_ACTIVATED_MANUALLY", "ACCOUNT", locked.accountId(), Map.of("requestId", requestId.toString()));
		return getForActor(manager, requestId, AccountRole.MANAGER);
	}

	@Transactional
	ManualActivationResponse reject(UUID manager, UUID requestId, ManualActivationDecisionRequest request) {
		ensure(manager, AccountRole.MANAGER);
		limits.check("MANUAL_DECISION_ACTOR", manager.toString(), 30, Duration.ofMinutes(1));
		Instant now = clock.instant();
		RequestLock locked = lockRequest(requestId);
		if (locked.status() != ManualActivationStatus.PENDING)
			throw error(HttpStatus.CONFLICT, "MANUAL_ACTIVATION_REQUEST_DECIDED", "Manual activation request is no longer pending.");
		manualActivationRepository.rejectUpdateEmailVerificationManualActivationRequests(manager, request.decisionNote().trim(), Timestamp.from(now), Timestamp.from(now), requestId);
		audits.save(new AuditEvent(UUID.randomUUID(), manager, "MANUAL_ACTIVATION_REQUEST_REJECTED", "MANUAL_ACTIVATION_REQUEST", requestId, Map.of("accountId", locked.accountId().toString(), "requestedByAccountId", locked.requestedBy().toString(), "reasonCode", locked.reasonCode(), "verificationMethod", locked.verificationMethod())));
		return getForActor(manager, requestId, AccountRole.MANAGER);
	}

	private RequestLock lockRequest(UUID id) {
		List<RequestLock> rows = manualActivationRepository.lockRequestFindEmailVerificationManualActivationRequests(id);
		if (rows.isEmpty())
			throw error(HttpStatus.NOT_FOUND, "MANUAL_ACTIVATION_REQUEST_NOT_FOUND", "Manual activation request was not found.");
		return rows.getFirst();
	}

	private void terminateArtifacts(UUID account, Instant now) {
		manualActivationRepository.terminateArtifactsUpdateEmailVerificationChallenges(Timestamp.from(now), Timestamp.from(now), account);
		manualActivationRepository.terminateArtifactsUpdateEmailVerificationSessions(Timestamp.from(now), Timestamp.from(now), account);
		manualActivationRepository.terminateArtifactsUpdateEmailOutbox(Timestamp.from(now), account);
	}

	private void ensure(UUID id, AccountRole role) {
		if (!accounts.existsByIdAndRoleAndStatus(id, role, AccountStatus.ACTIVE))
			throw new InvalidAuthenticatedAccountException();
	}

	private RequesterSnapshot requesterSnapshot(UUID id) {
		return manualActivationRepository.requesterSnapshotFindAccounts(id);
	}

	private void validatePage(int page, int size) {
		if (page < 0 || size < 1 || size > 100)
			throw error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "page must be >= 0 and size between 1 and 100.");
	}

	private EmailVerificationException error(HttpStatus status, String code, String message) {
		return new EmailVerificationException(status, code, message);
	}

	record PendingAccount(UUID id, String memberCode, String email, Instant expiresAt) {
	}

	record RequesterSnapshot(String name, AccountRole role) {
	}

	record RequestLock(UUID accountId, UUID requestedBy, ManualActivationStatus status, Instant expiresAt, String reasonCode, String verificationMethod) {
	}

	private ManualActivationPageResponse activationPage(UUID actor, AccountRole role, ManualActivationStatus status, int page, int size) {
		return manualActivationRepository.activationPage(actor, role, status, page, size);
	}

	private ManualActivationResponse activationDetail(UUID actor, UUID id, AccountRole role) {
		return manualActivationRepository.activationDetail(actor, id, role);
	}

	private final ManualActivationRepository manualActivationRepository;
}
