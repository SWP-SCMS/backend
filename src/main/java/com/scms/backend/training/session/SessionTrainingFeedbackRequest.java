package com.scms.backend.training.session;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record SessionTrainingFeedbackRequest(@NotNull UUID trainingResultId, @NotBlank String content) {
}
