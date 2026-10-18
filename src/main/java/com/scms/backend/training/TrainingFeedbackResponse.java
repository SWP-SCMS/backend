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
	Instant updatedAt
) {
	static TrainingFeedbackResponse from(TrainingFeedback feedback) {
		return from(feedback, null);
	}

	static TrainingFeedbackResponse from(TrainingFeedback feedback, UUID classSessionId) {
		return new TrainingFeedbackResponse(feedback.getId(), feedback.getMemberAccountId(),
				feedback.getCoachAssignmentId(), feedback.getTrainingPlanVersionId(),
				feedback.getTrainingResultId(), classSessionId, feedback.getContent(), feedback.getCreatedByAccountId(),
				feedback.getCreatedAt(), feedback.getUpdatedAt());
	}
}
