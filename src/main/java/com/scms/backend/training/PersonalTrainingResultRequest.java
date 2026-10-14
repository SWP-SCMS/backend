package com.scms.backend.training;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record PersonalTrainingResultRequest(
	@NotNull UUID trainingPlanVersionId,
	@NotNull Instant activityAt,
	@NotNull Map<String, Object> content
) {
}
