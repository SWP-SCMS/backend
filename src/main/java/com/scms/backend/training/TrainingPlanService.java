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
		if (!accounts.existsByIdAndRoleAndStatus(coachId, AccountRole.COACH, AccountStatus.ACTIVE)) {
			throw TrainingPlanException.actorNotActiveCoach();
		}

		Instant now = clock.instant();
		assignments.endActiveAssignmentIfPlusExpired(memberId, now);
		if (!assignments.hasActivePlusMembership(memberId, now)) {
			throw TrainingPlanException.plusMembershipRequired();
		}

		CoachAssignment assignment = assignments
				.findByMemberAccountIdAndCoachAccountIdAndStatus(memberId, coachId, CoachAssignmentStatus.ACTIVE)
				.orElseThrow(TrainingPlanException::activeAssignmentNotFound);

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
}
