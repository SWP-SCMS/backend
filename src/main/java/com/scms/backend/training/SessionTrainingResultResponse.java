package com.scms.backend.training;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record SessionTrainingResultResponse(
	UUID id,
	UUID memberAccountId,
	UUID classSessionId,
	Instant activityAt,
	Map<String, Object> content,
	UUID createdByAccountId,
	Instant createdAt,
	Instant updatedAt
) {
	static SessionTrainingResultResponse from(TrainingResult result) {
		return new SessionTrainingResultResponse(result.getId(), result.getMemberAccountId(),
			result.getClassSessionId(), result.getActivityAt(), result.getContent(),
			result.getCreatedByAccountId(), result.getCreatedAt(), result.getUpdatedAt());
	}
}
