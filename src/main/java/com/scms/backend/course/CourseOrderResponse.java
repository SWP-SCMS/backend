package com.scms.backend.course;

import java.math.BigInteger;
import java.time.Instant;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

public record CourseOrderResponse(
	UUID orderId,
	@Schema(pattern = "^COR-[0-9A-F]{32}$") String orderReference,
	String memberId,
	UUID courseRunId,
	BigInteger tuitionVnd,
	@Schema(allowableValues = "VND") String currencyCode,
	@Schema(allowableValues = { "PENDING_PAYMENT", "EXPIRED" }) String orderStatus,
	@Schema(allowableValues = { "HELD", "EXPIRED" }) String reservationStatus,
	Instant expiresAt,
	Instant createdAt) {
}
