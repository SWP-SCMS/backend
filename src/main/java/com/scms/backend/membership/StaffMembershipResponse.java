package com.scms.backend.membership;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

public record StaffMembershipResponse(
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID membershipId,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED) String planCode,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED) String offerName,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = { "ACTIVE", "EXPIRED" }) String status,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant startsAt,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant endsAt,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<String> benefitCodes,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<MembershipBenefitResponse> benefits) {

	StaffMembershipResponse(UUID membershipId, String planCode, String offerName, String status, Instant startsAt,
			Instant endsAt) {
		this(membershipId, planCode, offerName, status, startsAt, endsAt, List.of(), List.of());
	}
}
