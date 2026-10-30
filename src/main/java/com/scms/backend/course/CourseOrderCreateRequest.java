package com.scms.backend.course;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
public record CourseOrderCreateRequest(
	@Schema(format = "uuid", requiredMode = Schema.RequiredMode.REQUIRED) @NotNull UUID courseRunId) {

	@JsonAnySetter
	public void rejectUnknownField(String fieldName, Object ignoredValue) {
		throw new IllegalArgumentException("Unknown course order field: " + fieldName);
	}
}
