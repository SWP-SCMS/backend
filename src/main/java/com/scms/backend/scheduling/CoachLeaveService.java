package com.scms.backend.scheduling;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.auth.InvalidAuthenticatedAccountException;
import com.scms.backend.audit.AuditEvent;
import com.scms.backend.audit.AuditEventRepository;
import com.scms.backend.notification.NotificationWriter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class CoachLeaveService {
	private final CoachLeaveRepository leaves;
	private final AccountRepository accounts;
	private final ClassSessionRepository sessions;
	private final AuditEventRepository audits;
	private final NotificationWriter notifications;
	private final CoachLeaveLifecycleService lifecycle;
	private final Clock clock;

	CoachLeaveService(CoachLeaveRepository leaves, AccountRepository accounts, ClassSessionRepository sessions,
			AuditEventRepository audits, NotificationWriter notifications, Clock clock) {
		this(leaves, accounts, sessions, audits, notifications, null, clock);
	}

	@org.springframework.beans.factory.annotation.Autowired
	CoachLeaveService(CoachLeaveRepository leaves, AccountRepository accounts, ClassSessionRepository sessions,
			AuditEventRepository audits, NotificationWriter notifications, CoachLeaveLifecycleService lifecycle,
			Clock clock) {
		this.leaves = leaves; this.accounts = accounts; this.sessions = sessions; this.audits = audits;
		this.notifications = notifications; this.lifecycle = lifecycle; this.clock = clock;
	}

	@Transactional
	CoachLeaveResponse create(UUID coachId, CoachLeaveCreateRequest request) {
		Account coach = activeAccount(coachId, AccountRole.COACH);
		if (request == null) throw CoachLeaveException.validation("request", "must be a JSON object");
		Instant now = clock.instant();
		if (request.startTime() == null || !request.startTime().isAfter(now))
			throw CoachLeaveException.validation("startTime", "must be in the future");
		if (request.endTime() == null || !request.endTime().isAfter(request.startTime()))
			throw CoachLeaveException.validation("endTime", "must be later than startTime");
		String reason = normalize(request.reason(), true, "reason");
		if (leaves.existsOpenOverlap(coachId, request.startTime(), request.endTime()))
			throw CoachLeaveException.conflict("COACH_LEAVE_OVERLAP", "An open leave request already overlaps this interval.");
		CoachLeaveRequest leave = new CoachLeaveRequest(UUID.randomUUID(), coach, request.startTime(), request.endTime(), reason);
		try { leaves.saveAndFlush(leave); }
		catch (DataIntegrityViolationException exception) {
			throw CoachLeaveException.conflict("COACH_LEAVE_OVERLAP", "An open leave request already overlaps this interval.");
		}
		audits.save(new AuditEvent(UUID.randomUUID(), coachId, "COACH_LEAVE_REQUESTED", "COACH_LEAVE_REQUEST",
			leave.getId(), reason, null, snapshot(leave)));
		notifyManagers(leave, "COACH_LEAVE_REQUESTED");
		return response(leave);
	}

	@Transactional
	List<CoachLeaveResponse> mine(UUID coachId) {
		activeAccount(coachId, AccountRole.COACH);
		catchUp();
		return leaves.findByCoach_IdOrderByCreatedAtDesc(coachId).stream().map(this::response).toList();
	}

	@Transactional
	CoachLeaveResponse withdraw(UUID coachId, UUID requestId) {
		activeAccount(coachId, AccountRole.COACH);
		expireIfStarted(requestId);
		CoachLeaveRequest leave = leaves.findByIdForUpdate(requestId).filter(item -> item.getCoach().getId().equals(coachId))
			.orElseThrow(CoachLeaveException::notFound);
		if (leave.getStatus() != CoachLeaveStatus.PENDING)
			throw CoachLeaveException.conflict("COACH_LEAVE_NOT_PENDING", "Only a pending leave request can be withdrawn.");
		Map<String, Object> before = snapshot(leave); leave.withdraw();
		audits.save(new AuditEvent(UUID.randomUUID(), coachId, "COACH_LEAVE_WITHDRAWN", "COACH_LEAVE_REQUEST",
			leave.getId(), "Coach withdrew leave request", before, snapshot(leave)));
		notifyManagers(leave, "COACH_LEAVE_WITHDRAWN");
		return response(leave);
	}

	@Transactional
	CoachLeaveResponse cancel(UUID coachId, UUID requestId) {
		activeAccount(coachId, AccountRole.COACH);
		CoachLeaveRequest leave = leaves.findByIdForUpdate(requestId)
			.filter(item -> item.getCoach().getId().equals(coachId))
			.orElseThrow(CoachLeaveException::notFound);
		if (leave.getStatus() != CoachLeaveStatus.APPROVED) throw CoachLeaveException.notApproved();
		Instant now = clock.instant();
		if (!now.isBefore(leave.getStartTime())) throw CoachLeaveException.cancellationAlreadyStarted();
		Map<String, Object> before = snapshot(leave);
		UUID approvingManagerId = leave.getReviewedBy();
		leave.cancel(now);
		audits.save(new AuditEvent(UUID.randomUUID(), coachId, "COACH_LEAVE_CANCELLED",
			"COACH_LEAVE_REQUEST", leave.getId(), "Coach cancelled approved leave", before, snapshot(leave)));
		notifications.write("coach-leave:" + leave.getId() + ":CANCELLED", approvingManagerId,
			"COACH_LEAVE_CANCELLED", "COACH_LEAVE_REQUEST", leave.getId(),
			Map.of("leaveRequestId", leave.getId().toString(), "coachId", coachId.toString(),
				"coachFullName", leave.getCoach().getFullName(), "startTime", leave.getStartTime().toString(),
				"endTime", leave.getEndTime().toString(), "status", leave.getStatus().name()));
		return response(leave);
	}

	@Transactional
	List<CoachLeaveResponse> managerList(UUID managerId, CoachLeaveStatus status) {
		activeAccount(managerId, AccountRole.MANAGER);
		catchUp();
		List<CoachLeaveRequest> result = status == null ? leaves.findAll() : leaves.findByStatusOrderByCreatedAtAsc(status);
		return result.stream().map(this::response).toList();
	}

	@Transactional
	CoachLeaveResponse approve(UUID managerId, UUID requestId, CoachLeaveReviewRequest request) {
		return review(managerId, requestId, request, true);
	}

	@Transactional
	CoachLeaveResponse reject(UUID managerId, UUID requestId, CoachLeaveReviewRequest request) {
		return review(managerId, requestId, request, false);
	}

	private CoachLeaveResponse review(UUID managerId, UUID requestId, CoachLeaveReviewRequest request, boolean approve) {
		activeAccount(managerId, AccountRole.MANAGER);
		expireIfStarted(requestId);
		CoachLeaveRequest leave = leaves.findByIdForUpdate(requestId).orElseThrow(CoachLeaveException::notFound);
		if (approve && leave.getStatus() == CoachLeaveStatus.EXPIRED) throw CoachLeaveException.alreadyStarted();
		if (leave.getStatus() != CoachLeaveStatus.PENDING)
			throw CoachLeaveException.conflict("COACH_LEAVE_NOT_PENDING", "Only a pending leave request can be reviewed.");
		String note = normalize(request == null ? null : request.note(), false, "note");
		Map<String, Object> before = snapshot(leave); Instant now = clock.instant();
		if (approve) {
			accounts.findByIdForUpdate(leave.getCoach().getId())
				.orElseThrow(InvalidAuthenticatedAccountException::new);
			List<UUID> affected = sessions.findActiveOverlapIdsByCoach(leave.getCoach().getId(),
				leave.getStartTime(), leave.getEndTime());
			if (!affected.isEmpty()) throw CoachLeaveException.affectedSessions(affected);
			leave.approve(managerId, note, now);
		}
		else leave.reject(managerId, note, now);
		String event = approve ? "COACH_LEAVE_APPROVED" : "COACH_LEAVE_REJECTED";
		audits.save(new AuditEvent(UUID.randomUUID(), managerId, event, "COACH_LEAVE_REQUEST", leave.getId(), note,
			before, snapshot(leave)));
		notifications.write("coach-leave:" + leave.getId() + ":" + leave.getStatus(), leave.getCoach().getId(), event,
			"COACH_LEAVE_REQUEST", leave.getId(), Map.of("startTime", leave.getStartTime().toString(),
				"endTime", leave.getEndTime().toString(), "status", leave.getStatus().name()));
		return response(leave);
	}

	private void catchUp() {
		if (lifecycle != null) lifecycle.catchUp(clock.instant());
	}

	private void expireIfStarted(UUID requestId) {
		if (lifecycle != null) lifecycle.expireIfStarted(requestId, clock.instant());
	}

	private void notifyManagers(CoachLeaveRequest leave, String event) {
		Map<String, Object> payload = Map.of("leaveRequestId", leave.getId().toString(),
			"coachId", leave.getCoach().getId().toString(), "coachFullName", leave.getCoach().getFullName(),
			"startTime", leave.getStartTime().toString(), "endTime", leave.getEndTime().toString(),
			"status", leave.getStatus().name());
		for (Account manager : accounts.findByRoleAndStatusOrderById(AccountRole.MANAGER, AccountStatus.ACTIVE)) {
			UUID recipient = manager.getId();
			notifications.write("coach-leave:" + leave.getId() + ":" + event + ":" + recipient,
				recipient, event, "COACH_LEAVE_REQUEST", leave.getId(), payload);
		}
	}

	private Account activeAccount(UUID id, AccountRole role) {
		Account account = accounts.findById(id).orElseThrow(InvalidAuthenticatedAccountException::new);
		if (account.getRole() != role || account.getStatus() != AccountStatus.ACTIVE)
			throw new InvalidAuthenticatedAccountException();
		return account;
	}
	private String normalize(String value, boolean required, String field) {
		String normalized = value == null ? null : value.trim();
		if (required && (normalized == null || normalized.isEmpty())) throw CoachLeaveException.validation(field, "is required");
		if (normalized != null && normalized.isEmpty()) normalized = null;
		if (normalized != null && normalized.length() > 1000) throw CoachLeaveException.validation(field, "must be at most 1000 characters");
		return normalized;
	}
	private CoachLeaveResponse response(CoachLeaveRequest leave) {
		return CoachLeaveResponse.from(leave, sessions.findActiveOverlapIdsByCoach(leave.getCoach().getId(),
			leave.getStartTime(), leave.getEndTime()));
	}
	private Map<String, Object> snapshot(CoachLeaveRequest leave) {
		return Map.of("coachId", leave.getCoach().getId().toString(), "startTime", leave.getStartTime().toString(),
			"endTime", leave.getEndTime().toString(), "status", leave.getStatus().name());
	}
}

