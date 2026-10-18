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
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TrainingResultService {

	private final TrainingResultRepository results;
	private final TrainingPlanVersionRepository planVersions;
	private final CoachAssignmentRepository assignments;
	private final AccountRepository accounts;
	private final AuditEventRepository audits;
	private final Clock clock;

	public TrainingResultService(TrainingResultRepository results, TrainingPlanVersionRepository planVersions,
			CoachAssignmentRepository assignments, AccountRepository accounts, AuditEventRepository audits,
			Clock clock) {
		this.results = results;
		this.planVersions = planVersions;
		this.assignments = assignments;
		this.accounts = accounts;
		this.audits = audits;
		this.clock = clock;
	}

	@Transactional
	public TrainingResultResponse create(UUID coachId, UUID memberId, PersonalTrainingResultRequest request) {
		Instant now = clock.instant();
		CoachAssignment assignment = requireCurrentAssignment(coachId, memberId, now, "TRAINING_RESULT");
		requireMemberPlanVersion(request.trainingPlanVersionId(), memberId);

		TrainingResult result = results.saveAndFlush(new TrainingResult(UUID.randomUUID(), memberId,
				assignment.getId(), request.trainingPlanVersionId(), request.activityAt(), request.content(),
				coachId, now));
		audits.save(new AuditEvent(UUID.randomUUID(), coachId, "TRAINING_RESULT_CREATED", "TRAINING_RESULT",
				result.getId(), snapshot(result)));
		return TrainingResultResponse.from(result);
	}

	@Transactional
	public TrainingResultResponse update(UUID coachId, UUID memberId, UUID resultId,
			PersonalTrainingResultRequest request) {
		Instant now = clock.instant();
		requireCurrentAssignment(coachId, memberId, now, "TRAINING_RESULT");
		requireMemberPlanVersion(request.trainingPlanVersionId(), memberId);

		TrainingResult result = results.findPersonalByIdAndMemberAccountIdForUpdate(resultId, memberId)
				.orElseThrow(TrainingResultException::resultNotFound);
		Map<String, Object> beforeData = snapshot(result);
		result.update(request.trainingPlanVersionId(), request.activityAt(), request.content(), now);
		results.saveAndFlush(result);
		audits.save(new AuditEvent(UUID.randomUUID(), coachId, "TRAINING_RESULT_UPDATED", "TRAINING_RESULT",
				resultId, null, beforeData, snapshot(result)));
		return TrainingResultResponse.from(result);
	}

	@Transactional
	public TrainingResultPageResponse list(UUID coachId, UUID memberId, Pageable pageable) {
		requireCurrentAssignment(coachId, memberId, clock.instant(), null);
		var page = results.findHistoryByMemberAccountId(memberId, pageable);
		return new TrainingResultPageResponse(page.getContent().stream()
				.map(TrainingResultResponse::from).toList(), page.getNumber(), page.getSize(),
				page.getTotalElements(), page.getTotalPages());
	}

	private CoachAssignment requireCurrentAssignment(UUID coachId, UUID memberId, Instant now,
			String requiredBenefit) {
		if (!accounts.existsByIdAndRoleAndStatus(coachId, AccountRole.COACH, AccountStatus.ACTIVE)) {
			throw TrainingResultException.actorNotActiveCoach();
		}
		assignments.endActiveAssignmentIfPersonalCoachingExpired(memberId, now);
		if (requiredBenefit != null
				&& !assignments.hasActiveMembershipBenefit(memberId, requiredBenefit, now)) {
			throw TrainingResultException.plusMembershipRequired();
		}
		return assignments.findByMemberAccountIdAndCoachAccountIdAndStatus(
				memberId, coachId, CoachAssignmentStatus.ACTIVE)
				.orElseThrow(TrainingResultException::activeAssignmentNotFound);
	}

	private void requireMemberPlanVersion(UUID versionId, UUID memberId) {
		if (planVersions.findByIdAndMemberAccountId(versionId, memberId).isEmpty()) {
			throw TrainingResultException.planVersionNotFound();
		}
	}

	private Map<String, Object> snapshot(TrainingResult result) {
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("memberAccountId", result.getMemberAccountId().toString());
		data.put("coachAssignmentId", result.getCoachAssignmentId().toString());
		data.put("trainingPlanVersionId", result.getTrainingPlanVersionId().toString());
		data.put("activityAt", result.getActivityAt().toString());
		data.put("content", result.getContent());
		return data;
	}
}
