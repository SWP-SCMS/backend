package com.scms.backend.reception;

import java.time.Instant;
import java.util.UUID;

public record CenterVisitResponse(UUID id, UUID memberId, String memberCode, UUID membershipId,
		UUID checkedInBy, Instant checkedInAt) {

	static CenterVisitResponse from(CenterVisit visit, String memberCode) {
		return new CenterVisitResponse(visit.getId(), visit.getMemberAccountId(), memberCode,
			visit.getMembershipId(), visit.getCheckedInByAccountId(), visit.getCheckedInAt());
	}
}
