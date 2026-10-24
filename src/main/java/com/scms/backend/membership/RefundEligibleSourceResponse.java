package com.scms.backend.membership;

import java.math.BigInteger;
import java.time.LocalDate;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

public record RefundEligibleSourceResponse(
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID paymentId,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID membershipId,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID receiptId,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) String receiptNumber,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) String orderNumber,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) String offerName,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) String memberCode,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) String memberFullName,
		@Schema(nullable = true, requiredMode = Schema.RequiredMode.REQUIRED) String paymentReference,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) RefundType type,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) BigInteger paidAmount,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) BigInteger suggestedAmount,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) String currency,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDate calculationDate) { }
