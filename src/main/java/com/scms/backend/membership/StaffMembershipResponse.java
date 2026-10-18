package com.scms.backend.membership;

import java.time.Instant;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

public record StaffMembershipResponse(
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID membershipId,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED) String planCode,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED) String offerName,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = { "ACTIVE", "EXPIRED" }) String status,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant startsAt,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant endsAt) {
}
