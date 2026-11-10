package com.scms.backend.course.order;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Schema(additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
public record CourseOrderCreateRequest(@Schema(format = "uuid", requiredMode = Schema.RequiredMode.REQUIRED) @NotNull UUID courseRunId) {

	@JsonAnySetter
	public void rejectUnknownField(String fieldName, Object ignoredValue) {
		throw new IllegalArgumentException("Unknown course order field: " + fieldName);
	}
}
