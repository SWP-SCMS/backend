package com.scms.backend.training.plan;

import com.scms.backend.training.TrainingPlan;
import com.scms.backend.training.TrainingPlanStatus;
import com.scms.backend.training.TrainingPlanVersion;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record TrainingPlanResponse(UUID id, UUID memberAccountId, UUID coachAssignmentId, String title, TrainingPlanStatus status, UUID createdByAccountId, Instant createdAt, Instant updatedAt, UUID versionId, int versionNumber, Instant versionCreatedAt, @Schema(accessMode = Schema.AccessMode.READ_ONLY, allowableValues = "1") int contentSchemaVersion, @Schema(implementation = TrainingPlanContentSchema.class) Map<String, Object> content) {

	static TrainingPlanResponse from(TrainingPlan plan, TrainingPlanVersion version) {
		return new TrainingPlanResponse(plan.getId(), plan.getMemberAccountId(), plan.getCoachAssignmentId(), plan.getTitle(), plan.getStatus(), plan.getCreatedByAccountId(), plan.getCreatedAt(), plan.getUpdatedAt(), version.getId(), version.getVersionNumber(), version.getCreatedAt(), 1, version.getContent());
	}
}
