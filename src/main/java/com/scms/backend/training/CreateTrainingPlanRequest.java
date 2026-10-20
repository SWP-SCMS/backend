package com.scms.backend.training;

import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

public record CreateTrainingPlanRequest(
	@NotBlank @Size(max = 200) String title,
	@NotNull @Schema(implementation = TrainingPlanContentSchema.class) Map<String, Object> content
) {
}
