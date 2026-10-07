package com.scms.backend.reception;

import java.time.Instant;
import java.util.UUID;

public record CenterVisitResponse(UUID id, UUID memberId, String memberCode, UUID membershipId,
		UUID checkedInBy, Instant checkedInAt, Instant checkedOutAt, CheckoutSource checkoutSource,
		boolean created) {

	static CenterVisitResponse from(CenterVisit visit, String memberCode, boolean created) {
		return new CenterVisitResponse(visit.getId(), visit.getMemberAccountId(), memberCode,
			visit.getMembershipId(), visit.getCheckedInByAccountId(), visit.getCheckedInAt(),
			visit.getCheckedOutAt(), visit.getCheckoutSource(), created);
	}
}
