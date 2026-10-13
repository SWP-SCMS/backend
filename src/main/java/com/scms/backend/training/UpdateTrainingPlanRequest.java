package com.scms.backend.training;

import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateTrainingPlanRequest(
	@NotBlank @Size(max = 200) String title,
	@NotNull Map<String, Object> content
) {
}
