package com.scms.backend.centervisit;

import java.time.Instant;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

public record CenterVisitResponse(UUID id, UUID memberId, String memberCode,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) String memberName,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) UUID membershipId,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) CenterVisitSource source,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) UUID courseEnrollmentId,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) UUID courseSessionId,
		UUID checkedInBy, Instant checkedInAt, Instant checkedOutAt, CheckoutSource checkoutSource,
		boolean created) {

	static CenterVisitResponse from(CenterVisit visit, String memberCode, String memberName, boolean created) {
		return new CenterVisitResponse(visit.getId(), visit.getMemberAccountId(), memberCode, memberName,
			visit.getMembershipId(), visit.getSource(), visit.getCourseEnrollmentId(), visit.getCourseSessionId(),
			visit.getCheckedInByAccountId(), visit.getCheckedInAt(),
			visit.getCheckedOutAt(), visit.getCheckoutSource(), created);
	}
}
