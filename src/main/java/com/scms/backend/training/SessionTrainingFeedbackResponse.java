package com.scms.backend.training;

import java.time.Instant;
import java.util.UUID;

public record SessionTrainingFeedbackResponse(
	UUID id,
	UUID memberAccountId,
	UUID classSessionId,
	UUID trainingResultId,
	String content,
	UUID createdByAccountId,
	Instant createdAt,
	Instant updatedAt
) {
	static SessionTrainingFeedbackResponse from(TrainingFeedback feedback, UUID classSessionId) {
		return new SessionTrainingFeedbackResponse(feedback.getId(), feedback.getMemberAccountId(),
			classSessionId, feedback.getTrainingResultId(), feedback.getContent(),
			feedback.getCreatedByAccountId(), feedback.getCreatedAt(), feedback.getUpdatedAt());
	}
}
