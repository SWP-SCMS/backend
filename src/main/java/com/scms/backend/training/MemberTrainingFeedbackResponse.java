package com.scms.backend.training;

import java.time.Instant;
import java.util.UUID;

public record MemberTrainingFeedbackResponse(
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
	static MemberTrainingFeedbackResponse from(TrainingFeedback feedback, UUID classSessionId) {
		return new MemberTrainingFeedbackResponse(feedback.getId(), feedback.getMemberAccountId(),
			feedback.getCoachAssignmentId(), feedback.getTrainingPlanVersionId(),
			feedback.getTrainingResultId(), classSessionId, feedback.getContent(),
			feedback.getCreatedByAccountId(), feedback.getCreatedAt(), feedback.getUpdatedAt());
	}
}
