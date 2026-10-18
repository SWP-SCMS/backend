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
import com.scms.backend.scheduling.BookingRepository;
import com.scms.backend.scheduling.BookingStatus;
import com.scms.backend.scheduling.ClassSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SessionTrainingResultService {

	private final TrainingResultRepository results;
	private final CoachAssignmentRepository assignments;
	private final ClassSessionRepository sessions;
	private final BookingRepository bookings;
	private final AccountRepository accounts;
	private final AuditEventRepository audits;
	private final Clock clock;

	public SessionTrainingResultService(TrainingResultRepository results, CoachAssignmentRepository assignments,
			ClassSessionRepository sessions, BookingRepository bookings, AccountRepository accounts,
			AuditEventRepository audits, Clock clock) {
		this.results = results;
		this.assignments = assignments;
		this.sessions = sessions;
		this.bookings = bookings;
		this.accounts = accounts;
		this.audits = audits;
		this.clock = clock;
	}

	@Transactional
	public SessionTrainingResultResponse create(UUID coachId, UUID sessionId, UUID memberId,
			SessionTrainingResultRequest request) {
		requireOwnedGroupOrYogaSession(coachId, sessionId);
		if (!bookings.existsByMemberAccountIdAndClassSessionIdAndStatus(memberId, sessionId,
				BookingStatus.BOOKED)) {
			throw TrainingResultException.sessionMemberNotFound();
		}
		requireTrainingResultBenefit(memberId);
		TrainingResult existing = results.findSessionByClassSessionIdAndMemberAccountIdForUpdate(sessionId, memberId)
				.orElse(null);
		if (existing != null) {
			return SessionTrainingResultResponse.from(existing);
		}

		Instant now = clock.instant();
		TrainingResult result = results.saveAndFlush(TrainingResult.forClassSession(UUID.randomUUID(), memberId,
			sessionId, request.activityAt(), request.content(), coachId, now));
		audits.save(new AuditEvent(UUID.randomUUID(), coachId, "TRAINING_RESULT_CREATED", "TRAINING_RESULT",
			result.getId(), snapshot(result)));
		return SessionTrainingResultResponse.from(result);
	}

	@Transactional
	public SessionTrainingResultResponse update(UUID coachId, UUID sessionId, UUID memberId, UUID resultId,
			SessionTrainingResultRequest request) {
		requireOwnedGroupOrYogaSession(coachId, sessionId);
		requireTrainingResultBenefit(memberId);
		TrainingResult result = results.findSessionByIdAndMemberAccountIdForUpdate(resultId, sessionId, memberId)
			.orElseThrow(TrainingResultException::sessionResultNotFound);
		Map<String, Object> beforeData = snapshot(result);
		result.updateClassSessionResult(request.activityAt(), request.content(), clock.instant());
		results.saveAndFlush(result);
		audits.save(new AuditEvent(UUID.randomUUID(), coachId, "TRAINING_RESULT_UPDATED", "TRAINING_RESULT",
			resultId, null, beforeData, snapshot(result)));
		return SessionTrainingResultResponse.from(result);
	}

	@Transactional(readOnly = true)
	public Optional<SessionTrainingResultResponse> get(UUID coachId, UUID sessionId, UUID memberId) {
		requireOwnedGroupOrYogaSessionForRead(coachId, sessionId);
		return results.findSessionByClassSessionIdAndMemberAccountId(sessionId, memberId)
				.map(SessionTrainingResultResponse::from);
	}

	private void requireOwnedGroupOrYogaSession(UUID coachId, UUID sessionId) {
		if (!accounts.existsByIdAndRoleAndStatus(coachId, AccountRole.COACH, AccountStatus.ACTIVE)) {
			throw TrainingResultException.actorNotActiveCoach();
		}
		sessions.findOwnedGroupOrYogaByIdForRead(sessionId, coachId)
			.orElseThrow(TrainingResultException::sessionNotFound);
	}

	private void requireOwnedGroupOrYogaSessionForRead(UUID coachId, UUID sessionId) {
		if (!accounts.existsByIdAndRoleAndStatus(coachId, AccountRole.COACH, AccountStatus.ACTIVE)) {
			throw TrainingResultException.actorNotActiveCoach();
		}
		sessions.findOwnedGroupOrYogaById(sessionId, coachId)
			.orElseThrow(TrainingResultException::sessionNotFound);
	}

	private void requireTrainingResultBenefit(UUID memberId) {
		if (!assignments.hasActiveMembershipBenefit(memberId, "TRAINING_RESULT", clock.instant())) {
			throw TrainingResultException.plusMembershipRequired();
		}
	}

	private Map<String, Object> snapshot(TrainingResult result) {
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("memberAccountId", result.getMemberAccountId().toString());
		data.put("classSessionId", result.getClassSessionId().toString());
		data.put("activityAt", result.getActivityAt().toString());
		data.put("content", result.getContent());
		return data;
	}
}
