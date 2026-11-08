package com.scms.backend.training;

import java.time.Instant;
import java.util.UUID;

public record TrainingFeedbackResponse(
	UUID id,
	UUID memberAccountId,
	UUID coachAssignmentId,
	UUID trainingPlanVersionId,
	UUID trainingResultId,
	UUID classSessionId,
	String content,
	UUID createdByAccountId,
	Instant createdAt,
	Instant updatedAt,
	UUID courseSessionId,
	Instant sessionStartTime
) {
	static TrainingFeedbackResponse from(TrainingFeedback feedback) {
		return from(feedback, null);
	}

	static TrainingFeedbackResponse from(TrainingFeedback feedback, UUID classSessionId) {
		return from(feedback, classSessionId, null);
	}

	static TrainingFeedbackResponse from(TrainingFeedback feedback, UUID classSessionId, UUID courseSessionId) {
		return from(feedback, classSessionId, courseSessionId, null);
	}

	static TrainingFeedbackResponse from(TrainingFeedback feedback, UUID classSessionId, UUID courseSessionId, Instant sessionStartTime) {
		return new TrainingFeedbackResponse(feedback.getId(), feedback.getMemberAccountId(),
				feedback.getCoachAssignmentId(), feedback.getTrainingPlanVersionId(),
				feedback.getTrainingResultId(), classSessionId, feedback.getContent(), feedback.getCreatedByAccountId(),
				feedback.getCreatedAt(), feedback.getUpdatedAt(), courseSessionId, sessionStartTime);
	}
}
