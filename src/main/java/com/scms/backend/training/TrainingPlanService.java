package com.scms.backend.training;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.audit.AuditEvent;
import com.scms.backend.audit.AuditEventRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TrainingPlanService {

	private final TrainingPlanRepository plans;
	private final TrainingPlanVersionRepository versions;
	private final CoachAssignmentRepository assignments;
	private final AccountRepository accounts;
	private final AuditEventRepository audits;
	private final Clock clock;

	public TrainingPlanService(TrainingPlanRepository plans, TrainingPlanVersionRepository versions,
			CoachAssignmentRepository assignments, AccountRepository accounts, AuditEventRepository audits,
			Clock clock) {
		this.plans = plans;
		this.versions = versions;
		this.assignments = assignments;
		this.accounts = accounts;
		this.audits = audits;
		this.clock = clock;
	}

	@Transactional
	public TrainingPlanResponse create(UUID coachId, UUID memberId, CreateTrainingPlanRequest request) {
		ensureActiveCoach(coachId);
		Instant now = clock.instant();
		CoachAssignment assignment = currentAssignment(coachId, memberId, now);

		TrainingPlan plan = plans.saveAndFlush(new TrainingPlan(UUID.randomUUID(), memberId, assignment.getId(),
				request.title().trim(), coachId, now));
		TrainingPlanVersion version = versions.saveAndFlush(new TrainingPlanVersion(UUID.randomUUID(), plan.getId(),
				1, request.content(), coachId, now));

		Map<String, Object> afterData = new LinkedHashMap<>();
		afterData.put("memberAccountId", memberId.toString());
		afterData.put("coachAssignmentId", assignment.getId().toString());
		afterData.put("title", plan.getTitle());
		afterData.put("status", plan.getStatus().name());
		afterData.put("versionId", version.getId().toString());
		afterData.put("versionNumber", version.getVersionNumber());
		audits.save(new AuditEvent(UUID.randomUUID(), coachId, "TRAINING_PLAN_CREATED", "TRAINING_PLAN",
				plan.getId(), afterData));

		return TrainingPlanResponse.from(plan, version);
	}

	@Transactional
	public TrainingPlanResponse update(UUID coachId, UUID memberId, UUID planId,
			UpdateTrainingPlanRequest request) {
		ensureActiveCoach(coachId);
		Instant now = clock.instant();
		currentAssignment(coachId, memberId, now);

		TrainingPlan plan = plans.findByIdAndMemberAccountIdForUpdate(planId, memberId)
				.orElseThrow(TrainingPlanException::planNotFound);
		if (plan.getStatus() != TrainingPlanStatus.ACTIVE) {
			throw TrainingPlanException.planNotActive();
		}

		TrainingPlanVersion previous = versions.findTopByTrainingPlanIdOrderByVersionNumberDesc(planId)
				.orElseThrow(TrainingPlanException::versionHistoryNotFound);
		String previousTitle = plan.getTitle();
		plan.update(request.title().trim(), now);
		plans.saveAndFlush(plan);

		TrainingPlanVersion next = versions.saveAndFlush(new TrainingPlanVersion(UUID.randomUUID(), planId,
				previous.getVersionNumber() + 1, request.content(), coachId, now));

		Map<String, Object> beforeData = new LinkedHashMap<>();
		beforeData.put("title", previousTitle);
		beforeData.put("versionId", previous.getId().toString());
		beforeData.put("versionNumber", previous.getVersionNumber());
		Map<String, Object> afterData = new LinkedHashMap<>();
		afterData.put("title", plan.getTitle());
		afterData.put("versionId", next.getId().toString());
		afterData.put("versionNumber", next.getVersionNumber());
		audits.save(new AuditEvent(UUID.randomUUID(), coachId, "TRAINING_PLAN_UPDATED", "TRAINING_PLAN",
				planId, null, beforeData, afterData));

		return TrainingPlanResponse.from(plan, next);
	}

	private void ensureActiveCoach(UUID coachId) {
		if (!accounts.existsByIdAndRoleAndStatus(coachId, AccountRole.COACH, AccountStatus.ACTIVE)) {
			throw TrainingPlanException.actorNotActiveCoach();
		}
	}

	private CoachAssignment currentAssignment(UUID coachId, UUID memberId, Instant now) {
		assignments.endActiveAssignmentIfPlusExpired(memberId, now);
		if (!assignments.hasActivePlusMembership(memberId, now)) {
			throw TrainingPlanException.plusMembershipRequired();
		}
		return assignments.findByMemberAccountIdAndCoachAccountIdAndStatus(
				memberId, coachId, CoachAssignmentStatus.ACTIVE)
				.orElseThrow(TrainingPlanException::activeAssignmentNotFound);
	}
}
