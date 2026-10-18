package com.scms.backend.training;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.audit.AuditEvent;
import com.scms.backend.audit.AuditEventRepository;
import com.scms.backend.scheduling.ClassSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SessionTrainingFeedbackService {

	private final TrainingFeedbackRepository feedback;
	private final TrainingResultRepository results;
	private final ClassSessionRepository sessions;
	private final AccountRepository accounts;
	private final AuditEventRepository audits;
	private final Clock clock;

	public SessionTrainingFeedbackService(TrainingFeedbackRepository feedback,
			TrainingResultRepository results, ClassSessionRepository sessions,
			AccountRepository accounts, AuditEventRepository audits, Clock clock) {
		this.feedback = feedback;
		this.results = results;
		this.sessions = sessions;
		this.accounts = accounts;
		this.audits = audits;
		this.clock = clock;
	}

	@Transactional
	public SessionTrainingFeedbackResponse create(UUID coachId, UUID sessionId, UUID memberId,
			SessionTrainingFeedbackRequest request) {
		requireOwnedGroupOrYogaSession(coachId, sessionId);
		requireSessionResult(request.trainingResultId(), sessionId, memberId);
		TrainingFeedback existing = feedback.findSessionByTrainingResultIdAndMemberAccountIdForUpdate(
				request.trainingResultId(), memberId).orElse(null);
		if (existing != null) {
			return SessionTrainingFeedbackResponse.from(existing, sessionId);
		}
		Instant now = clock.instant();
		TrainingFeedback created = feedback.saveAndFlush(TrainingFeedback.forClassSessionResult(
			UUID.randomUUID(), memberId, request.trainingResultId(), request.content(), coachId, now));
		audits.save(new AuditEvent(UUID.randomUUID(), coachId, "TRAINING_FEEDBACK_CREATED",
			"TRAINING_FEEDBACK", created.getId(), snapshot(created, sessionId)));
		return SessionTrainingFeedbackResponse.from(created, sessionId);
	}

	@Transactional
	public SessionTrainingFeedbackResponse update(UUID coachId, UUID sessionId, UUID memberId,
			UUID feedbackId, SessionTrainingFeedbackRequest request) {
		requireOwnedGroupOrYogaSession(coachId, sessionId);
		requireSessionResult(request.trainingResultId(), sessionId, memberId);
		TrainingFeedback current = feedback.findSessionByIdAndMemberAccountIdForUpdate(
			feedbackId, sessionId, memberId).orElseThrow(TrainingFeedbackException::sessionFeedbackNotFound);
		Map<String, Object> beforeData = snapshot(current, sessionId);
		current.updateClassSessionResult(request.trainingResultId(), request.content(), clock.instant());
		feedback.saveAndFlush(current);
		audits.save(new AuditEvent(UUID.randomUUID(), coachId, "TRAINING_FEEDBACK_UPDATED",
			"TRAINING_FEEDBACK", feedbackId, null, beforeData, snapshot(current, sessionId)));
		return SessionTrainingFeedbackResponse.from(current, sessionId);
	}

	@Transactional(readOnly = true)
	public Optional<SessionTrainingFeedbackResponse> get(UUID coachId, UUID sessionId, UUID memberId) {
		requireOwnedGroupOrYogaSessionForRead(coachId, sessionId);
		return feedback.findSessionByClassSessionIdAndMemberAccountId(sessionId, memberId)
				.map(value -> SessionTrainingFeedbackResponse.from(value, sessionId));
	}

	private void requireOwnedGroupOrYogaSession(UUID coachId, UUID sessionId) {
		if (!accounts.existsByIdAndRoleAndStatus(coachId, AccountRole.COACH, AccountStatus.ACTIVE)) {
			throw TrainingFeedbackException.actorNotActiveCoach();
		}
		sessions.findOwnedGroupOrYogaByIdForRead(sessionId, coachId)
			.orElseThrow(TrainingFeedbackException::sessionNotFound);
	}

	private void requireOwnedGroupOrYogaSessionForRead(UUID coachId, UUID sessionId) {
		if (!accounts.existsByIdAndRoleAndStatus(coachId, AccountRole.COACH, AccountStatus.ACTIVE)) {
			throw TrainingFeedbackException.actorNotActiveCoach();
		}
		sessions.findOwnedGroupOrYogaById(sessionId, coachId)
			.orElseThrow(TrainingFeedbackException::sessionNotFound);
	}

	private void requireSessionResult(UUID resultId, UUID sessionId, UUID memberId) {
		results.findSessionByIdAndMemberAccountIdForUpdate(resultId, sessionId, memberId)
			.orElseThrow(TrainingFeedbackException::sessionResultNotFound);
	}

	private Map<String, Object> snapshot(TrainingFeedback value, UUID sessionId) {
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("memberAccountId", value.getMemberAccountId().toString());
		data.put("classSessionId", sessionId.toString());
		data.put("trainingResultId", value.getTrainingResultId().toString());
		data.put("content", value.getContent());
		return data;
	}
}
