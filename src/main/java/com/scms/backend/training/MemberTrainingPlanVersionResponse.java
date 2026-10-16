package com.scms.backend.training;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record MemberTrainingPlanVersionResponse(
	UUID id,
	int versionNumber,
	Map<String, Object> content,
	UUID createdByAccountId,
	Instant createdAt
) {
	static MemberTrainingPlanVersionResponse from(TrainingPlanVersion version) {
		return new MemberTrainingPlanVersionResponse(version.getId(), version.getVersionNumber(),
			version.getContent(), version.getCreatedByAccountId(), version.getCreatedAt());
	}
}
