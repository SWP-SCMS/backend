package com.scms.backend.identity.receptionmember;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.scms.backend.membership.MembershipStatus;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

public record ReceptionActiveMembershipResponse(UUID membershipId, String offerName, String planName,
		@Schema(description = "Membership start date in Asia/Ho_Chi_Minh") LocalDate effectiveStartDate,
		@Schema(description = "Exclusive Membership end date in Asia/Ho_Chi_Minh") LocalDate effectiveEndDate,
		@Schema(allowableValues = "ACTIVE") MembershipStatus status,
		@ArraySchema(uniqueItems = true, arraySchema = @Schema(description = "Distinct benefit codes sorted ascending"))
		List<String> benefitCodes, boolean frozen) {
}
