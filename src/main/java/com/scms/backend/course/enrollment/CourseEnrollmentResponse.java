package com.scms.backend.course.enrollment;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigInteger;
import java.time.Instant;
import java.util.UUID;

@Schema(additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
public record CourseEnrollmentResponse(@Schema(format = "uuid", requiredMode = Schema.RequiredMode.REQUIRED) UUID enrollmentId, @Schema(format = "uuid", requiredMode = Schema.RequiredMode.REQUIRED) UUID orderId, @Schema(format = "uuid", requiredMode = Schema.RequiredMode.REQUIRED) UUID memberId, @Schema(format = "uuid", requiredMode = Schema.RequiredMode.REQUIRED) UUID courseRunId, @Schema(minimum = "1", maximum = "9999999999999999999", requiredMode = Schema.RequiredMode.REQUIRED) BigInteger amountPaidVnd, @Schema(minimum = "1", requiredMode = Schema.RequiredMode.REQUIRED) int totalUnits, @Schema(format = "date-time", requiredMode = Schema.RequiredMode.REQUIRED) Instant enrolledAt) {
}
