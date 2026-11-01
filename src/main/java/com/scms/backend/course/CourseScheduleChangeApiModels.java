package com.scms.backend.course;

import java.time.Instant;
import java.math.BigInteger;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

public final class CourseScheduleChangeApiModels {
	private CourseScheduleChangeApiModels() {}

	@Schema(additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
	public record ScheduleChange(UUID id, UUID enrollmentSessionId, Instant oldStartAt, Instant oldEndAt,
			Instant newStartAt, Instant newEndAt, Instant notificationSentAt, Instant responseDeadline,
			String status, String response, Instant respondedAt, UUID changeEventId, CourseWithdrawalCause cause,
			UUID withdrawalRequestId, boolean withdrawalEligible, String withdrawalReason,
			BigInteger refundBasisVnd, BigInteger refundAmountVnd) {
		public ScheduleChange(UUID id, UUID enrollmentSessionId, Instant oldStartAt, Instant oldEndAt,
				Instant newStartAt, Instant newEndAt, Instant notificationSentAt, Instant responseDeadline,
				String status, String response, Instant respondedAt) {
			this(id, enrollmentSessionId, oldStartAt, oldEndAt, newStartAt, newEndAt, notificationSentAt,
				responseDeadline, status, response, respondedAt, null, CourseWithdrawalCause.VOLUNTARY,
				null, false, null, null, null);
		}
	}

	@Schema(additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
	public record ScheduleChangeResponse(String response) {}
}
