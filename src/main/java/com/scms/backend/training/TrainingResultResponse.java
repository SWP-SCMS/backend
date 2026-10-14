package com.scms.backend.training;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record TrainingResultResponse(
	UUID id,
	UUID memberAccountId,
	UUID coachAssignmentId,
	UUID trainingPlanVersionId,
	Instant activityAt,
	Map<String, Object> content,
	UUID createdByAccountId,
	Instant createdAt,
	Instant updatedAt
) {
	static TrainingResultResponse from(TrainingResult result) {
		return new TrainingResultResponse(result.getId(), result.getMemberAccountId(),
				result.getCoachAssignmentId(), result.getTrainingPlanVersionId(), result.getActivityAt(),
				result.getContent(), result.getCreatedByAccountId(), result.getCreatedAt(), result.getUpdatedAt());
	}
}
