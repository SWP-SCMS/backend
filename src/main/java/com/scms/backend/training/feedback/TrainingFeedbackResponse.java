package com.scms.backend.training.feedback;

import com.scms.backend.training.TrainingFeedback;
import java.time.Instant;
import java.util.UUID;

public record TrainingFeedbackResponse(UUID id, UUID memberAccountId, UUID coachAssignmentId, UUID trainingPlanVersionId, UUID trainingResultId, UUID classSessionId, String content, UUID createdByAccountId, Instant createdAt, Instant updatedAt, UUID courseSessionId, Instant sessionStartTime) {

	public static TrainingFeedbackResponse from(TrainingFeedback feedback) {
		return from(feedback, null);
	}

	public static TrainingFeedbackResponse from(TrainingFeedback feedback, UUID classSessionId) {
		return from(feedback, classSessionId, null);
	}

	public static TrainingFeedbackResponse from(TrainingFeedback feedback, UUID classSessionId, UUID courseSessionId) {
		return from(feedback, classSessionId, courseSessionId, null);
	}

	public static TrainingFeedbackResponse from(TrainingFeedback feedback, UUID classSessionId, UUID courseSessionId, Instant sessionStartTime) {
		return new TrainingFeedbackResponse(feedback.getId(), feedback.getMemberAccountId(), feedback.getCoachAssignmentId(), feedback.getTrainingPlanVersionId(), feedback.getTrainingResultId(), classSessionId, feedback.getContent(), feedback.getCreatedByAccountId(), feedback.getCreatedAt(), feedback.getUpdatedAt(), courseSessionId, sessionStartTime);
	}
}
