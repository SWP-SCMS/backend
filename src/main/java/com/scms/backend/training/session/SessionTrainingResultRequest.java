package com.scms.backend.training.session;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Map;

public record SessionTrainingResultRequest(@NotNull Instant activityAt, @NotNull Map<String, Object> content) {
}
