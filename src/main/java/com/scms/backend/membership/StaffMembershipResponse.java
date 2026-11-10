package com.scms.backend.membership;

import com.scms.backend.membership.plan.MembershipBenefitResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record StaffMembershipResponse(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID membershipId, @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String planCode, @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String offerName, @Schema(requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = { "RESERVED", "ACTIVE", "EXPIRED", "TERMINATED", "CANCELLED" }) String status, @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) Instant startsAt, @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) Instant endsAt, @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<String> benefitCodes, @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<MembershipBenefitResponse> benefits) {

	public StaffMembershipResponse(UUID membershipId, String planCode, String offerName, String status, Instant startsAt, Instant endsAt) {
		this(membershipId, planCode, offerName, status, startsAt, endsAt, List.of(), List.of());
	}
}
