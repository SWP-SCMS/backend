package com.scms.backend.training;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MemberTrainingPlanResponse(
	UUID id,
	UUID memberAccountId,
	UUID coachAssignmentId,
	String title,
	TrainingPlanStatus status,
	UUID createdByAccountId,
	Instant createdAt,
	Instant updatedAt,
	List<MemberTrainingPlanVersionResponse> versions
) {
	static MemberTrainingPlanResponse from(TrainingPlan plan, List<TrainingPlanVersion> versions) {
		return new MemberTrainingPlanResponse(plan.getId(), plan.getMemberAccountId(),
				plan.getCoachAssignmentId(), plan.getTitle(), plan.getStatus(), plan.getCreatedByAccountId(),
				plan.getCreatedAt(), plan.getUpdatedAt(), versions.stream()
						.map(MemberTrainingPlanVersionResponse::from)
						.toList());
	}
}
