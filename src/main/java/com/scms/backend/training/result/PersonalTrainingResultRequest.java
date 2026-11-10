package com.scms.backend.training.result;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record PersonalTrainingResultRequest(@NotNull UUID trainingPlanVersionId, @NotNull Instant activityAt, @NotNull Map<String, Object> content) {
}
