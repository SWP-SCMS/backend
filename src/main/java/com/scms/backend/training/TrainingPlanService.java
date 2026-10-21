package com.scms.backend.training;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.audit.AuditEvent;
import com.scms.backend.audit.AuditEventRepository;

import org.springframework.stereotype.Service;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;

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
		TrainingPlanContentValidator.validate(request.content());
		Instant now = clock.instant();
		CoachAssignment assignment = currentAssignment(coachId, memberId, now, "TRAINING_PLAN");
		boolean replacing = request.replaceActivePlanId() != null || request.expectedActivePlanVersion() != null;
		if ((request.replaceActivePlanId() == null) != (request.expectedActivePlanVersion() == null)) {
			throw TrainingPlanException.validationError("replaceActivePlanId",
				"replaceActivePlanId and expectedActivePlanVersion must be provided together.");
		}
		TrainingPlan active = plans.findActiveByMemberAccountIdForUpdate(memberId).orElse(null);
		TrainingPlanVersion previous = active == null ? null
			: versions.findTopByTrainingPlanIdOrderByVersionNumberDesc(active.getId())
				.orElseThrow(TrainingPlanException::versionHistoryNotFound);
		if (!replacing && active != null) {
			throw TrainingPlanException.activePlanExists(active, previous.getVersionNumber());
		}
		if (replacing && (active == null || !active.getId().equals(request.replaceActivePlanId())
				|| previous.getVersionNumber() != request.expectedActivePlanVersion())) {
			throw TrainingPlanException.activePlanChanged();
		}
		if (active != null) {
			active.archive(now);
			plans.saveAndFlush(active);
		}

		TrainingPlan plan;
		try {
			plan = plans.saveAndFlush(new TrainingPlan(UUID.randomUUID(), memberId, assignment.getId(),
				request.title().trim(), coachId, now));
		}
		catch (DataIntegrityViolationException exception) {
			throw TrainingPlanException.activePlanChanged();
		}
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
		if (active != null) {
			Map<String, Object> replacement = new LinkedHashMap<>();
			replacement.put("memberAccountId", memberId.toString());
			replacement.put("coachAssignmentId", assignment.getId().toString());
			replacement.put("archivedPlanId", active.getId().toString());
			replacement.put("archivedPlanVersion", previous.getVersionNumber());
			replacement.put("newPlanId", plan.getId().toString());
			audits.save(new AuditEvent(UUID.randomUUID(), coachId, "TRAINING_PLAN_REPLACED", "TRAINING_PLAN",
				plan.getId(), replacement));
		}

		return TrainingPlanResponse.from(plan, version);
	}

	@Transactional
	public TrainingPlanResponse update(UUID coachId, UUID memberId, UUID planId,
			UpdateTrainingPlanRequest request) {
		ensureActiveCoach(coachId);
		TrainingPlanContentValidator.validate(request.content());
		Instant now = clock.instant();
		currentAssignment(coachId, memberId, now, "TRAINING_PLAN");

		TrainingPlan plan = plans.findByIdAndMemberAccountIdForUpdate(planId, memberId)
				.orElseThrow(TrainingPlanException::planNotFound);
		if (plan.getStatus() != TrainingPlanStatus.ACTIVE) {
			throw TrainingPlanException.planNotActive();
		}

		TrainingPlanVersion previous = versions.findTopByTrainingPlanIdOrderByVersionNumberDesc(planId)
				.orElseThrow(TrainingPlanException::versionHistoryNotFound);
		if (previous.getVersionNumber() != request.expectedVersionNumber()) {
			throw TrainingPlanException.versionConflict(previous.getVersionNumber());
		}
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

	@Transactional
	public List<TrainingPlanVersionResponse> versions(UUID coachId, UUID memberId, UUID planId) {
		ensureActiveCoach(coachId);
		currentAssignment(coachId, memberId, clock.instant(), null);
		plans.findByIdAndMemberAccountId(planId, memberId).orElseThrow(TrainingPlanException::planNotFound);
		List<TrainingPlanVersion> history = versions.findByTrainingPlanIdOrderByVersionNumberDesc(planId);
		if (history.isEmpty()) throw TrainingPlanException.versionHistoryNotFound();
		return history.stream().map(TrainingPlanVersionResponse::from).toList();
	}

	@Transactional
	public TrainingPlanResponse archive(UUID coachId, UUID memberId, UUID planId) {
		ensureActiveCoach(coachId);
		Instant now = clock.instant();
		CoachAssignment assignment = currentAssignment(coachId, memberId, now, null);
		TrainingPlan plan = plans.findByIdAndMemberAccountIdForUpdate(planId, memberId)
				.orElseThrow(TrainingPlanException::planNotFound);
		TrainingPlanVersion latest = versions.findTopByTrainingPlanIdOrderByVersionNumberDesc(planId)
				.orElseThrow(TrainingPlanException::versionHistoryNotFound);
		if (plan.getStatus() == TrainingPlanStatus.ARCHIVED) {
			return TrainingPlanResponse.from(plan, latest);
		}
		plan.archive(now);
		plans.saveAndFlush(plan);
		Map<String, Object> beforeData = Map.of("status", TrainingPlanStatus.ACTIVE.name());
		Map<String, Object> afterData = new LinkedHashMap<>();
		afterData.put("status", TrainingPlanStatus.ARCHIVED.name());
		afterData.put("memberAccountId", memberId.toString());
		afterData.put("coachAssignmentId", assignment.getId().toString());
		audits.save(new AuditEvent(UUID.randomUUID(), coachId, "TRAINING_PLAN_ARCHIVED", "TRAINING_PLAN",
				planId, null, beforeData, afterData));
		return TrainingPlanResponse.from(plan, latest);
	}

	@Transactional
	public TrainingPlanPageResponse list(UUID coachId, UUID memberId, Pageable pageable) {
		ensureActiveCoach(coachId);
		currentAssignment(coachId, memberId, clock.instant(), null);
		var page = plans.findByMemberAccountId(memberId, pageable);
		List<UUID> planIds = page.getContent().stream().map(TrainingPlan::getId).toList();
		Map<UUID, TrainingPlanVersion> latestVersions = planIds.isEmpty() ? Map.of()
				: versions.findHistoryByTrainingPlanIds(planIds).stream()
					.collect(Collectors.toMap(TrainingPlanVersion::getTrainingPlanId, Function.identity(),
							(left, right) -> left.getVersionNumber() > right.getVersionNumber() ? left : right));
		List<TrainingPlanResponse> content = page.getContent().stream()
				.map(plan -> {
					TrainingPlanVersion latest = latestVersions.get(plan.getId());
					if (latest == null) throw TrainingPlanException.versionHistoryNotFound();
					return TrainingPlanResponse.from(plan, latest);
				})
				.toList();
		return new TrainingPlanPageResponse(content, page.getNumber(), page.getSize(),
				page.getTotalElements(), page.getTotalPages());
	}

	private void ensureActiveCoach(UUID coachId) {
		if (!accounts.existsByIdAndRoleAndStatus(coachId, AccountRole.COACH, AccountStatus.ACTIVE)) {
			throw TrainingPlanException.actorNotActiveCoach();
		}
	}

	private CoachAssignment currentAssignment(UUID coachId, UUID memberId, Instant now, String requiredBenefit) {
		assignments.endActiveAssignmentIfPersonalCoachingExpired(memberId, now);
		if (requiredBenefit != null
				&& !assignments.hasActiveMembershipBenefit(memberId, requiredBenefit, now)) {
			throw TrainingPlanException.plusMembershipRequired();
		}
		return assignments.findByMemberAccountIdAndCoachAccountIdAndStatus(
				memberId, coachId, CoachAssignmentStatus.ACTIVE)
				.orElseThrow(TrainingPlanException::activeAssignmentNotFound);
	}
}
