package com.scms.backend.course;

import java.time.Instant;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

public final class CourseScheduleChangeApiModels {
	private CourseScheduleChangeApiModels() {}

	@Schema(additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
	public record ScheduleChange(UUID id, UUID enrollmentSessionId, Instant oldStartAt, Instant oldEndAt,
			Instant newStartAt, Instant newEndAt, Instant notificationSentAt, Instant responseDeadline,
			String status, String response, Instant respondedAt) {}

	@Schema(additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
	public record ScheduleChangeResponse(String response) {}
}
