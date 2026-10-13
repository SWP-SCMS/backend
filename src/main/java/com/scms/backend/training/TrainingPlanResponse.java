package com.scms.backend.training;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record TrainingPlanResponse(
	UUID id,
	UUID memberAccountId,
	UUID coachAssignmentId,
	String title,
	TrainingPlanStatus status,
	UUID createdByAccountId,
	Instant createdAt,
	UUID versionId,
	int versionNumber,
	Map<String, Object> content
) {
	static TrainingPlanResponse from(TrainingPlan plan, TrainingPlanVersion version) {
		return new TrainingPlanResponse(plan.getId(), plan.getMemberAccountId(), plan.getCoachAssignmentId(),
				plan.getTitle(), plan.getStatus(), plan.getCreatedByAccountId(), plan.getCreatedAt(),
				version.getId(), version.getVersionNumber(), version.getContent());
	}
}
