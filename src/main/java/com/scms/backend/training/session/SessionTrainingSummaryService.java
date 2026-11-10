package com.scms.backend.training.session;

import com.scms.backend.scheduling.session.ClassSessionRepository;
import com.scms.backend.training.TrainingResultException;
import java.util.LinkedHashMap;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SessionTrainingSummaryService {

	private final SessionTrainingResultService results;

	private final SessionTrainingFeedbackService feedback;

	private final ClassSessionRepository sessions;

	public SessionTrainingSummaryService(SessionTrainingResultService results, SessionTrainingFeedbackService feedback, ClassSessionRepository sessions) {
		this.results = results;
		this.feedback = feedback;
		this.sessions = sessions;
	}

	@Transactional
	public Summary save(UUID coachId, UUID sessionId, UUID memberId, String notes, String comment) {
		// Serialize creates/retries on the session and keep both writes in one transaction.
		var session = sessions.findOwnedGroupOrYogaByIdForRead(sessionId, coachId).orElseThrow(TrainingResultException::sessionNotFound);
		var previous = results.get(coachId, sessionId, memberId).orElse(null);
		var content = new LinkedHashMap<String, Object>();
		if (previous != null)
			content.putAll(previous.content());
		content.put("notes", notes.trim());
		var request = new SessionTrainingResultRequest(previous == null ? session.getStartTime() : previous.activityAt(), content);
		var result = previous == null ? results.create(coachId, sessionId, memberId, request) : results.update(coachId, sessionId, memberId, previous.id(), request);
		var oldFeedback = feedback.get(coachId, sessionId, memberId).orElse(null);
		var feedbackRequest = new SessionTrainingFeedbackRequest(result.id(), comment.trim());
		var savedFeedback = oldFeedback == null ? feedback.create(coachId, sessionId, memberId, feedbackRequest) : feedback.update(coachId, sessionId, memberId, oldFeedback.id(), feedbackRequest);
		return new Summary(result, savedFeedback);
	}

	public record Summary(SessionTrainingResultResponse result, SessionTrainingFeedbackResponse feedback) {
	}
}
