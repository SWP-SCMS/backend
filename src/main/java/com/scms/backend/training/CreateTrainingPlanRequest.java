package com.scms.backend.training;

import java.util.Map;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Positive;
import io.swagger.v3.oas.annotations.media.Schema;

public record CreateTrainingPlanRequest(
	@NotBlank @Size(max = 200) String title,
	@NotNull @Schema(implementation = TrainingPlanContentSchema.class) Map<String, Object> content,
	@Schema(description = "ACTIVE plan to archive atomically before creating this plan") UUID replaceActivePlanId,
	@Positive @Schema(description = "Latest version of replaceActivePlanId observed by the Coach") Integer expectedActivePlanVersion
) {
	public CreateTrainingPlanRequest(String title, Map<String, Object> content) {
		this(title, content, null, null);
	}
}
