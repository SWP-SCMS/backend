package com.scms.backend.training.feedback;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record PersonalTrainingFeedbackRequest(UUID trainingPlanVersionId, UUID trainingResultId, @NotBlank String content) {

	@AssertTrue(message = "Exactly one of trainingPlanVersionId or trainingResultId is required.")
	public boolean isContextValid() {
		return (trainingPlanVersionId == null) != (trainingResultId == null);
	}
}
