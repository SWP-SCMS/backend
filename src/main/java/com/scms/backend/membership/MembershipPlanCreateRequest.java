package com.scms.backend.membership;

import java.util.List;

public record MembershipPlanCreateRequest(String code, String displayName, MembershipPlanStatus status,
		List<String> benefitCodes) {
}
