package com.scms.backend.course;

import java.math.BigInteger;
import java.time.Instant;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
public record CourseOrderResponse(
	@Schema(format = "uuid", requiredMode = Schema.RequiredMode.REQUIRED) UUID orderId,
	@Schema(pattern = "^COR-[0-9A-F]{32}$", requiredMode = Schema.RequiredMode.REQUIRED) String orderReference,
	@Schema(format = "uuid", requiredMode = Schema.RequiredMode.REQUIRED) UUID memberId,
	@Schema(format = "uuid", requiredMode = Schema.RequiredMode.REQUIRED) UUID courseRunId,
	@Schema(minimum = "1", maximum = "9999999999999999999", requiredMode = Schema.RequiredMode.REQUIRED)
	BigInteger tuitionVnd,
	@Schema(allowableValues = "VND", requiredMode = Schema.RequiredMode.REQUIRED) String currencyCode,
	@Schema(allowableValues = { "PENDING_PAYMENT", "EXPIRED" }, requiredMode = Schema.RequiredMode.REQUIRED)
	String orderStatus,
	@Schema(allowableValues = { "HELD", "EXPIRED" }, requiredMode = Schema.RequiredMode.REQUIRED)
	String reservationStatus,
	@Schema(format = "date-time", requiredMode = Schema.RequiredMode.REQUIRED) Instant expiresAt,
	@Schema(format = "date-time", requiredMode = Schema.RequiredMode.REQUIRED) Instant createdAt) {
}
