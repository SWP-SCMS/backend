package com.scms.backend.training;

import java.util.UUID;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;

public record PersonalTrainingFeedbackRequest(
	UUID trainingPlanVersionId,
	UUID trainingResultId,
	@NotBlank String content
) {
	@AssertTrue(message = "Exactly one of trainingPlanVersionId or trainingResultId is required.")
	public boolean isContextValid() {
		return (trainingPlanVersionId == null) != (trainingResultId == null);
	}
}
