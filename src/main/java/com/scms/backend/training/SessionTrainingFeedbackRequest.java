package com.scms.backend.training;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SessionTrainingFeedbackRequest(
	@NotNull UUID trainingResultId,
	@NotBlank String content
) {
}
