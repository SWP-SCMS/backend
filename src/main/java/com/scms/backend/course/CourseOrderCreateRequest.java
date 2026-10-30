package com.scms.backend.course;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.NotNull;

public record CourseOrderCreateRequest(@NotNull UUID courseRunId) {

	@JsonAnySetter
	public void rejectUnknownField(String fieldName, Object ignoredValue) {
		throw new IllegalArgumentException("Unknown course order field: " + fieldName);
	}
}
