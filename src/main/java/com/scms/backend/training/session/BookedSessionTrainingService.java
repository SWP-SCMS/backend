package com.scms.backend.training.session;

import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.audit.AuditEvent;
import com.scms.backend.audit.AuditEventRepository;
import com.scms.backend.training.TrainingFeedback;
import com.scms.backend.training.TrainingResult;
import com.scms.backend.training.TrainingResultException;
import com.scms.backend.training.assignment.CoachAssignmentRepository;
import com.scms.backend.training.feedback.TrainingFeedbackRepository;
import com.scms.backend.training.feedback.TrainingFeedbackResponse;
import com.scms.backend.training.plan.TrainingPlanVersionRepository;
import com.scms.backend.training.result.TrainingResultRepository;
import com.scms.backend.training.result.TrainingResultResponse;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Results belong to a booked PT session or an enrollment in the current Course model.
 */
@Service
public class BookedSessionTrainingService {

	private final JdbcTemplate db;

	private final AccountRepository accounts;

	private final TrainingResultRepository results;

	private final TrainingFeedbackRepository feedback;

	private final TrainingPlanVersionRepository plans;

	private final CoachAssignmentRepository assignments;

	private final AuditEventRepository audits;

	private final Clock clock;

	public BookedSessionTrainingService(JdbcTemplate db, AccountRepository accounts, TrainingResultRepository results, TrainingFeedbackRepository feedback, TrainingPlanVersionRepository plans, CoachAssignmentRepository assignments, AuditEventRepository audits, Clock clock) {
		this.db = db;
		this.accounts = accounts;
		this.results = results;
		this.feedback = feedback;
		this.plans = plans;
		this.assignments = assignments;
		this.audits = audits;
		this.clock = clock;
		this.bookedSessionTrainingRepository = new BookedSessionTrainingRepository(this.db);
	}

	@Transactional(readOnly = true)
	public Summary get(UUID coachId, UUID sessionId, UUID memberId, boolean course) {
		Session session = session(coachId, sessionId, course, false);
		TrainingResult result = result(sessionId, memberId, course);
		boolean enrolled = enrolled(sessionId, memberId, course, false);
		if (!enrolled && result == null)
			throw TrainingResultException.sessionMemberNotFound();
		TrainingFeedback comment = result == null ? null : feedback.findCanonicalSessionFeedback(result.getId(), memberId).orElse(null);
		return response(result, comment, session.writable(clock.instant()) && enrolled);
	}

	@Transactional
	public Summary save(UUID coachId, UUID sessionId, UUID memberId, boolean course, UUID planVersionId, String notes, String comment) {
		// The session lock serializes concurrent saves and makes retries update the same pair.
		Session session = session(coachId, sessionId, course, true);
		if (!session.writable(clock.instant()))
			throw TrainingResultException.sessionNotEnded();
		if (!enrolled(sessionId, memberId, course, true))
			throw TrainingResultException.sessionMemberNotFound();
		if (!course) {
			if (planVersionId == null || plans.findByIdAndMemberAccountId(planVersionId, memberId).isEmpty())
				throw TrainingResultException.planVersionNotFound();
			if (!assignments.hasActiveMembershipBenefit(memberId, "TRAINING_RESULT", session.start()) || !assignments.hasActiveMembershipBenefit(memberId, "TRAINING_FEEDBACK", session.start()))
				throw TrainingResultException.plusMembershipRequired();
		} else if (planVersionId != null) {
			throw TrainingResultException.planVersionNotFound();
		}
		Instant now = clock.instant();
		TrainingResult result = result(sessionId, memberId, course);
		boolean creating = result == null;
		Map<String, Object> beforeResult = result == null ? null : resultSnapshot(result);
		Map<String, Object> content = new LinkedHashMap<>();
		if (result != null)
			content.putAll(result.getContent());
		content.put("notes", notes.trim());
		if (result == null) {
			result = course ? TrainingResult.forCourseSession(UUID.randomUUID(), memberId, sessionId, session.start(), content, coachId, now) : TrainingResult.forClassSession(UUID.randomUUID(), memberId, sessionId, session.start(), content, coachId, now);
		} else
			result.updateClassSessionResult(session.start(), content, now);
		result.setSessionPlanVersion(planVersionId);
		result = results.saveAndFlush(result);
		audits.save(new AuditEvent(UUID.randomUUID(), coachId, creating ? "TRAINING_RESULT_CREATED" : "TRAINING_RESULT_UPDATED", "TRAINING_RESULT", result.getId(), null, beforeResult, resultSnapshot(result)));
		TrainingFeedback saved = feedback.findCanonicalSessionFeedback(result.getId(), memberId).orElse(null);
		boolean newFeedback = saved == null;
		Map<String, Object> beforeFeedback = saved == null ? null : Map.of("content", saved.getContent(), "trainingResultId", result.getId().toString());
		if (saved == null)
			saved = TrainingFeedback.forClassSessionResult(UUID.randomUUID(), memberId, result.getId(), comment, coachId, now);
		else
			saved.updateClassSessionResult(result.getId(), comment, now);
		saved = feedback.saveAndFlush(saved);
		audits.save(new AuditEvent(UUID.randomUUID(), coachId, newFeedback ? "TRAINING_FEEDBACK_CREATED" : "TRAINING_FEEDBACK_UPDATED", "TRAINING_FEEDBACK", saved.getId(), null, beforeFeedback, Map.of("content", saved.getContent(), "trainingResultId", result.getId().toString())));
		return response(result, saved, true);
	}

	private TrainingResult result(UUID sessionId, UUID memberId, boolean course) {
		return (course ? results.findCourseSessionResult(sessionId, memberId) : results.findSessionByClassSessionIdAndMemberAccountId(sessionId, memberId)).orElse(null);
	}

	private Session session(UUID coachId, UUID id, boolean course, boolean lock) {
		if (!accounts.existsByIdAndRoleAndStatus(coachId, AccountRole.COACH, AccountStatus.ACTIVE))
			throw TrainingResultException.actorNotActiveCoach();
		return sessionRow(coachId, id, course, lock);
	}

	private boolean enrolled(UUID sessionId, UUID memberId, boolean course, boolean lock) {
		return bookedSessionTrainingRepository.enrolled(sessionId, memberId, course, lock);
	}

	private Map<String, Object> resultSnapshot(TrainingResult result) {
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("memberAccountId", result.getMemberAccountId().toString());
		data.put("classSessionId", result.getClassSessionId());
		data.put("courseSessionId", result.getCourseSessionId());
		data.put("trainingPlanVersionId", result.getTrainingPlanVersionId());
		data.put("activityAt", result.getActivityAt().toString());
		data.put("content", result.getContent());
		return data;
	}

	private Summary response(TrainingResult result, TrainingFeedback comment, boolean writable) {
		return new Summary(result == null ? null : TrainingResultResponse.from(result), comment == null ? null : TrainingFeedbackResponse.from(comment, result.getClassSessionId(), result.getCourseSessionId(), result.getActivityAt()), writable);
	}

	record Session(Instant start, Instant end, String status, String runStatus) {

		boolean writable(Instant now) {
			return !now.isBefore(end) && (status.equals("SCHEDULED") || status.equals("COMPLETED") || status.equals("MAKEUP_SCHEDULED")) && (runStatus.equals("IN_PROGRESS") || runStatus.equals("CONFIRMED") || runStatus.equals("SETTLEMENT_PENDING") || runStatus.equals("COMPLETED"));
		}
	}

	public record Summary(TrainingResultResponse result, TrainingFeedbackResponse feedback, boolean writable) {
	}

	private Session sessionRow(UUID coachId, UUID id, boolean course, boolean lock) {
		return bookedSessionTrainingRepository.sessionRow(coachId, id, course, lock);
	}

	private final BookedSessionTrainingRepository bookedSessionTrainingRepository;
}
