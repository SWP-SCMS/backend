package com.scms.backend.training.session;

import com.scms.backend.training.feedback.TrainingFeedbackRepository;
import com.scms.backend.training.feedback.TrainingFeedbackResponse;
import com.scms.backend.training.result.TrainingResultRepository;
import com.scms.backend.training.result.TrainingResultResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MemberSessionTrainingService {

	private final TrainingResultRepository results;
	private final TrainingFeedbackRepository feedback;

	public MemberSessionTrainingService(TrainingResultRepository results, TrainingFeedbackRepository feedback) {
		this.results = results;
		this.feedback = feedback;
	}

	@Transactional(readOnly = true)
	public Summary get(UUID memberId, UUID sessionId, boolean course) {
		// Scope both reads to the authenticated member, including after membership expiry.
		var result = (course ? results.findCourseSessionResult(sessionId, memberId)
				: results.findSessionByClassSessionIdAndMemberAccountId(sessionId, memberId)).orElse(null);
		if (result == null) return new Summary(null, null);
		var comment = feedback.findCanonicalSessionFeedback(result.getId(), memberId)
				.map(value -> TrainingFeedbackResponse.from(value, result.getClassSessionId(), result.getCourseSessionId(), result.getActivityAt()))
				.orElse(null);
		return new Summary(TrainingResultResponse.from(result), comment);
	}

	@Schema(name = "MemberSessionTrainingSummary", description = "The authenticated member's result and feedback for one session. Missing entries are null; membership expiry does not hide history.")
	public record Summary(@Schema(types = { "object", "null" }) TrainingResultResponse result, @Schema(types = { "object", "null" }) TrainingFeedbackResponse feedback) {
	}
}
