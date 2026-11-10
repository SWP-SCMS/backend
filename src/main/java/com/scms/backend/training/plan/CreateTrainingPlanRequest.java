package com.scms.backend.training.plan;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.Map;
import java.util.UUID;

public record CreateTrainingPlanRequest(@NotBlank @Size(max = 200) String title, @NotNull @Schema(implementation = TrainingPlanContentSchema.class) Map<String, Object> content, @Schema(description = "ACTIVE plan to archive atomically before creating this plan") UUID replaceActivePlanId, @Positive @Schema(description = "Latest version of replaceActivePlanId observed by the Coach") Integer expectedActivePlanVersion) {

	public CreateTrainingPlanRequest(String title, Map<String, Object> content) {
		this(title, content, null, null);
	}
}
