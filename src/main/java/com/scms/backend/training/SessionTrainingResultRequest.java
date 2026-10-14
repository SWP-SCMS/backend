package com.scms.backend.training;

import java.time.Instant;
import java.util.Map;

import jakarta.validation.constraints.NotNull;

public record SessionTrainingResultRequest(
	@NotNull Instant activityAt,
	@NotNull Map<String, Object> content
) {
}
