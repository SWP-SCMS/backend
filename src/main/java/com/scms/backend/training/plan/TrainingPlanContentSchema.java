package com.scms.backend.training.plan;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(name = "TrainingPlanContent", description = "Schema version 1. Unknown request fields are rejected.")
public record TrainingPlanContentSchema(@Schema(maxLength = 4000) String goal, @Schema(maxLength = 4000) String schedule, @ArraySchema(maxItems = 100, schema = @Schema(maxLength = 500)) List<String> exercises, @Schema(maxLength = 4000) String notes) {
}
