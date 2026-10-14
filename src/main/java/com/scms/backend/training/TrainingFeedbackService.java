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
public class TrainingFeedbackService {

	private final TrainingFeedbackRepository feedback;
	private final TrainingPlanVersionRepository planVersions;
	private final TrainingResultRepository results;
	private final CoachAssignmentRepository assignments;
	private final AccountRepository accounts;
	private final AuditEventRepository audits;
	private final Clock clock;

	public TrainingFeedbackService(TrainingFeedbackRepository feedback,
			TrainingPlanVersionRepository planVersions, TrainingResultRepository results,
			CoachAssignmentRepository assignments, AccountRepository accounts,
			AuditEventRepository audits, Clock clock) {
		this.feedback = feedback;
		this.planVersions = planVersions;
		this.results = results;
		this.assignments = assignments;
		this.accounts = accounts;
		this.audits = audits;
		this.clock = clock;
	}

	@Transactional
	public TrainingFeedbackResponse create(UUID coachId, UUID memberId,
			PersonalTrainingFeedbackRequest request) {
		Instant now = clock.instant();
		CoachAssignment assignment = requireCurrentAssignment(coachId, memberId, now);
		requireOwnedContext(memberId, request);

		TrainingFeedback created = feedback.saveAndFlush(new TrainingFeedback(UUID.randomUUID(), memberId,
				assignment.getId(), request.trainingPlanVersionId(), request.trainingResultId(),
				request.content(), coachId, now));
		audits.save(new AuditEvent(UUID.randomUUID(), coachId, "TRAINING_FEEDBACK_CREATED",
				"TRAINING_FEEDBACK", created.getId(), snapshot(created)));
		return TrainingFeedbackResponse.from(created);
	}

	@Transactional
	public TrainingFeedbackResponse update(UUID coachId, UUID memberId, UUID feedbackId,
			PersonalTrainingFeedbackRequest request) {
		Instant now = clock.instant();
		requireCurrentAssignment(coachId, memberId, now);
		requireOwnedContext(memberId, request);

		TrainingFeedback current = feedback.findPersonalByIdAndMemberAccountIdForUpdate(feedbackId, memberId)
				.orElseThrow(TrainingFeedbackException::feedbackNotFound);
		Map<String, Object> beforeData = snapshot(current);
		current.update(request.trainingPlanVersionId(), request.trainingResultId(), request.content(), now);
		feedback.saveAndFlush(current);
		audits.save(new AuditEvent(UUID.randomUUID(), coachId, "TRAINING_FEEDBACK_UPDATED",
				"TRAINING_FEEDBACK", feedbackId, null, beforeData, snapshot(current)));
		return TrainingFeedbackResponse.from(current);
	}

	private CoachAssignment requireCurrentAssignment(UUID coachId, UUID memberId, Instant now) {
		if (!accounts.existsByIdAndRoleAndStatus(coachId, AccountRole.COACH, AccountStatus.ACTIVE)) {
			throw TrainingFeedbackException.actorNotActiveCoach();
		}
		assignments.endActiveAssignmentIfPlusExpired(memberId, now);
		if (!assignments.hasActivePlusMembership(memberId, now)) {
			throw TrainingFeedbackException.plusMembershipRequired();
		}
		return assignments.findByMemberAccountIdAndCoachAccountIdAndStatus(
				memberId, coachId, CoachAssignmentStatus.ACTIVE)
				.orElseThrow(TrainingFeedbackException::activeAssignmentNotFound);
	}

	private void requireOwnedContext(UUID memberId, PersonalTrainingFeedbackRequest request) {
		if (request.trainingPlanVersionId() != null
				&& planVersions.findByIdAndMemberAccountId(request.trainingPlanVersionId(), memberId).isEmpty()) {
			throw TrainingFeedbackException.planVersionNotFound();
		}
		if (request.trainingResultId() != null
				&& results.findPersonalByIdAndMemberAccountId(request.trainingResultId(), memberId).isEmpty()) {
			throw TrainingFeedbackException.resultNotFound();
		}
	}

	private Map<String, Object> snapshot(TrainingFeedback value) {
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("memberAccountId", value.getMemberAccountId().toString());
		data.put("coachAssignmentId", value.getCoachAssignmentId().toString());
		if (value.getTrainingPlanVersionId() != null) {
			data.put("trainingPlanVersionId", value.getTrainingPlanVersionId().toString());
		}
		if (value.getTrainingResultId() != null) {
			data.put("trainingResultId", value.getTrainingResultId().toString());
		}
		data.put("content", value.getContent());
		return data;
	}
}
