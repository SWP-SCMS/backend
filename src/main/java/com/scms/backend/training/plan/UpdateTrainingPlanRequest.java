package com.scms.backend.training.plan;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.Map;

public record UpdateTrainingPlanRequest(@NotBlank @Size(max = 200) String title, @NotNull @Schema(implementation = TrainingPlanContentSchema.class) Map<String, Object> content, @NotNull @Positive @Schema(description = "Version number loaded when editing began", minimum = "1") Integer expectedVersionNumber) {
}
