package com.scms.backend.training;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.audit.AuditEvent;
import com.scms.backend.audit.AuditEventRepository;
import com.scms.backend.notification.NotificationWriter;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CoachAssignmentService {

	private final CoachAssignmentRepository assignments;
	private final AccountRepository accounts;
	private final AuditEventRepository audits;
	private final NotificationWriter notifications;
	private final Clock clock;

	public CoachAssignmentService(CoachAssignmentRepository assignments, AccountRepository accounts,
			AuditEventRepository audits, NotificationWriter notifications, Clock clock) {
		this.assignments = assignments;
		this.accounts = accounts;
		this.audits = audits;
		this.notifications = notifications;
		this.clock = clock;
	}

	@Transactional
	public CoachAssignmentResponse assign(UUID managerId, UUID memberId, CoachAssignmentAssignRequest request) {
		ensureActiveManager(managerId);

		if (request == null || request.coachAccountId() == null) {
			throw CoachAssignmentException.validationError("coachAccountId", "coachAccountId must not be null");
		}
		if (request.reason() != null && request.reason().length() > 1000) {
			throw CoachAssignmentException.validationError("reason", "reason must be at most 1000 characters");
		}

		Account member = accounts.findById(memberId).orElseThrow(CoachAssignmentException::memberNotFound);
		if (member.getRole() != AccountRole.MEMBER) {
			throw CoachAssignmentException.memberNotFound();
		}
		if (member.getStatus() != AccountStatus.ACTIVE) {
			throw CoachAssignmentException.memberNotActive();
		}

		UUID coachId = request.coachAccountId();
		Account coach = accounts.findByIdForUpdate(coachId).or(() -> accounts.findById(coachId))
			.orElseThrow(CoachAssignmentException::coachNotFound);
		if (coach.getRole() != AccountRole.COACH) {
			throw CoachAssignmentException.coachNotFound();
		}
		if (coach.getStatus() != AccountStatus.ACTIVE) {
			throw CoachAssignmentException.coachNotActive();
		}

		Instant now = clock.instant();
		assignments.endActiveAssignmentIfPersonalCoachingExpired(memberId, now);
		if (!assignments.hasActiveMembershipBenefit(memberId, "PERSONAL_COACHING", now)) {
			throw CoachAssignmentException.plusMembershipRequired();
		}

		Optional<CoachAssignment> currentOpt = assignments.findByMemberAccountIdAndStatusForUpdate(memberId,
				CoachAssignmentStatus.ACTIVE);

		if (currentOpt.isPresent()) {
			CoachAssignment current = currentOpt.get();
			if (current.getCoachAccountId().equals(coachId)) {
				throw CoachAssignmentException.alreadyAssignedToThisCoach();
			}

			String endReason = request.reason() != null && !request.reason().isBlank()
					? request.reason().trim() : "Reassigned to another coach";
			current.end(now, endReason);
			assignments.saveAndFlush(current);

			CoachAssignment newAssignment = new CoachAssignment(UUID.randomUUID(), memberId, coachId, managerId,
					CoachAssignmentStatus.ACTIVE, now, now);
			try {
				newAssignment = assignments.saveAndFlush(newAssignment);
			}
			catch (DataIntegrityViolationException exception) {
				throw CoachAssignmentException.concurrentConflict();
			}

			String auditReason = request.reason() != null && !request.reason().isBlank()
					? request.reason().trim() : null;
			audits.save(new AuditEvent(UUID.randomUUID(), managerId, "COACH_REASSIGNED", "COACH_ASSIGNMENT",
					newAssignment.getId(), auditReason,
					Map.of("assignmentId", current.getId().toString(), "coachAccountId",
							current.getCoachAccountId().toString(), "status", CoachAssignmentStatus.ACTIVE.name()),
					Map.of("assignmentId", newAssignment.getId().toString(), "coachAccountId",
							newAssignment.getCoachAccountId().toString(), "status", CoachAssignmentStatus.ACTIVE.name())));

			// Notifications
			notifications.write("COACH_REASSIGNED:" + newAssignment.getId() + ":MEMBER", memberId,
					"TRAINING_COACH_ASSIGNED", "COACH_ASSIGNMENT", newAssignment.getId(),
					Map.of("coachAccountId", coachId.toString(), "coachFullName", coach.getFullName()));

			notifications.write("COACH_REASSIGNED:" + newAssignment.getId() + ":NEW_COACH", coachId,
					"TRAINING_COACH_ASSIGNED", "COACH_ASSIGNMENT", newAssignment.getId(),
					Map.of("memberAccountId", memberId.toString(), "memberFullName", member.getFullName()));

			notifications.write("COACH_ASSIGNMENT_ENDED:" + current.getId() + ":OLD_COACH",
					current.getCoachAccountId(), "TRAINING_COACH_ENDED", "COACH_ASSIGNMENT", current.getId(),
					Map.of("memberAccountId", memberId.toString(), "reason", endReason));

			return CoachAssignmentResponse.from(newAssignment, coach);
		}
		else {
			CoachAssignment newAssignment = new CoachAssignment(UUID.randomUUID(), memberId, coachId, managerId,
					CoachAssignmentStatus.ACTIVE, now, now);
			try {
				newAssignment = assignments.saveAndFlush(newAssignment);
			}
			catch (DataIntegrityViolationException exception) {
				throw CoachAssignmentException.concurrentConflict();
			}

			String auditReason = request.reason() != null && !request.reason().isBlank()
					? request.reason().trim() : null;
			audits.save(new AuditEvent(UUID.randomUUID(), managerId, "COACH_ASSIGNED", "COACH_ASSIGNMENT",
					newAssignment.getId(), auditReason, null,
					Map.of("assignmentId", newAssignment.getId().toString(), "memberAccountId",
							memberId.toString(), "coachAccountId", coachId.toString(), "status",
							CoachAssignmentStatus.ACTIVE.name())));

			// Notifications
			notifications.write("COACH_ASSIGNED:" + newAssignment.getId() + ":MEMBER", memberId,
					"TRAINING_COACH_ASSIGNED", "COACH_ASSIGNMENT", newAssignment.getId(),
					Map.of("coachAccountId", coachId.toString(), "coachFullName", coach.getFullName()));

			notifications.write("COACH_ASSIGNED:" + newAssignment.getId() + ":COACH", coachId,
					"TRAINING_COACH_ASSIGNED", "COACH_ASSIGNMENT", newAssignment.getId(),
					Map.of("memberAccountId", memberId.toString(), "memberFullName", member.getFullName()));

			return CoachAssignmentResponse.from(newAssignment, coach);
		}
	}

	@Transactional
	public Optional<CoachAssignmentResponse> getActive(UUID managerId, UUID memberId) {
		ensureActiveManager(managerId);
		ensureMemberExists(memberId);

		Instant now = clock.instant();
		assignments.endActiveAssignmentIfPersonalCoachingExpired(memberId, now);

		Optional<CoachAssignment> active = assignments.findByMemberAccountIdAndStatus(memberId,
				CoachAssignmentStatus.ACTIVE);
		if (active.isEmpty()) {
			return Optional.empty();
		}
		Account coach = accounts.findById(active.get().getCoachAccountId()).orElse(null);
		return Optional.of(CoachAssignmentResponse.from(active.get(), coach));
	}

	@Transactional
	public CoachAssignmentPageResponse listHistory(UUID managerId, UUID memberId, Pageable pageable) {
		ensureActiveManager(managerId);
		ensureMemberExists(memberId);

		Instant now = clock.instant();
		assignments.endActiveAssignmentIfPersonalCoachingExpired(memberId, now);

		Page<CoachAssignment> page = assignments.findByMemberAccountIdOrderByCreatedAtDesc(memberId, pageable);
		Set<UUID> coachIds = page.getContent().stream()
				.map(CoachAssignment::getCoachAccountId)
				.collect(Collectors.toSet());

		Map<UUID, Account> coachMap = accounts.findAllById(coachIds).stream()
				.collect(Collectors.toMap(Account::getId, a -> a));

		List<CoachAssignmentResponse> responses = page.getContent().stream()
				.map(a -> CoachAssignmentResponse.from(a, coachMap.get(a.getCoachAccountId())))
				.toList();

		return new CoachAssignmentPageResponse(responses, page.getNumber(), page.getSize(),
				page.getTotalElements(), page.getTotalPages());
	}

	@Transactional
	public CoachAssignmentResponse endAssignment(UUID managerId, UUID memberId, CoachAssignmentEndRequest request) {
		ensureActiveManager(managerId);
		ensureMemberExists(memberId);

		if (request == null || request.reason() == null || request.reason().trim().isEmpty()) {
			throw CoachAssignmentException.validationError("reason", "reason must be non-blank");
		}
		if (request.reason().trim().length() > 1000) {
			throw CoachAssignmentException.validationError("reason", "reason must be at most 1000 characters");
		}

		Instant now = clock.instant();
		assignments.endActiveAssignmentIfPersonalCoachingExpired(memberId, now);

		CoachAssignment current = assignments.findByMemberAccountIdAndStatusForUpdate(memberId,
				CoachAssignmentStatus.ACTIVE).orElseThrow(CoachAssignmentException::activeAssignmentNotFound);
		String reason = request.reason().trim();
		current.end(now, reason);
		assignments.saveAndFlush(current);

		audits.save(new AuditEvent(UUID.randomUUID(), managerId, "COACH_ASSIGNMENT_ENDED", "COACH_ASSIGNMENT",
				current.getId(), reason,
				Map.of("assignmentId", current.getId().toString(), "coachAccountId",
						current.getCoachAccountId().toString(), "status", CoachAssignmentStatus.ACTIVE.name()),
				Map.of("assignmentId", current.getId().toString(), "coachAccountId",
						current.getCoachAccountId().toString(), "status", CoachAssignmentStatus.ENDED.name())));

		// Notifications
		notifications.write("COACH_ASSIGNMENT_ENDED:" + current.getId() + ":MEMBER", memberId,
				"TRAINING_COACH_ENDED", "COACH_ASSIGNMENT", current.getId(),
				Map.of("coachAccountId", current.getCoachAccountId().toString(), "reason", reason));

		notifications.write("COACH_ASSIGNMENT_ENDED:" + current.getId() + ":COACH",
				current.getCoachAccountId(), "TRAINING_COACH_ENDED", "COACH_ASSIGNMENT", current.getId(),
				Map.of("memberAccountId", memberId.toString(), "reason", reason));

		Account coach = accounts.findById(current.getCoachAccountId()).orElse(null);
		return CoachAssignmentResponse.from(current, coach);
	}

	private void ensureActiveManager(UUID managerId) {
		Account manager = accounts.findById(managerId).orElseThrow(CoachAssignmentException::actorNotActiveManager);
		if (manager.getRole() != AccountRole.MANAGER || manager.getStatus() != AccountStatus.ACTIVE) {
			throw CoachAssignmentException.actorNotActiveManager();
		}
	}

	private void ensureMemberExists(UUID memberId) {
		Account member = accounts.findById(memberId).orElseThrow(CoachAssignmentException::memberNotFound);
		if (member.getRole() != AccountRole.MEMBER) {
			throw CoachAssignmentException.memberNotFound();
		}
	}
}
