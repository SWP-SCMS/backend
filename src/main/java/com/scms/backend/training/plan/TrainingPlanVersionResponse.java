package com.scms.backend.training.plan;

import com.scms.backend.training.TrainingPlanVersion;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record TrainingPlanVersionResponse(UUID id, int versionNumber, @Schema(implementation = TrainingPlanContentSchema.class) Map<String, Object> content, @Schema(accessMode = Schema.AccessMode.READ_ONLY, allowableValues = "1") int contentSchemaVersion, UUID createdByAccountId, Instant createdAt) {

	static TrainingPlanVersionResponse from(TrainingPlanVersion version) {
		return new TrainingPlanVersionResponse(version.getId(), version.getVersionNumber(), version.getContent(), 1, version.getCreatedByAccountId(), version.getCreatedAt());
	}
}
