package com.scms.backend.training;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record MemberTrainingResultResponse(
	UUID id,
	UUID memberAccountId,
	UUID coachAssignmentId,
	UUID trainingPlanVersionId,
	UUID classSessionId,
	Instant activityAt,
	Map<String, Object> content,
	UUID createdByAccountId,
	Instant createdAt,
	Instant updatedAt
) {
	static MemberTrainingResultResponse from(TrainingResult result) {
		return new MemberTrainingResultResponse(result.getId(), result.getMemberAccountId(),
			result.getCoachAssignmentId(), result.getTrainingPlanVersionId(), result.getClassSessionId(),
			result.getActivityAt(), result.getContent(), result.getCreatedByAccountId(), result.getCreatedAt(),
			result.getUpdatedAt());
	}
}
