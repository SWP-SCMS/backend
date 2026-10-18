package com.scms.backend.membership;

import java.util.List;

public record MembershipPlanPatchRequest(String code, String displayName, MembershipPlanStatus status,
		List<String> benefitCodes) {
	public MembershipPlanPatchRequest(String displayName, MembershipPlanStatus status, List<String> benefitCodes) {
		this(null, displayName, status, benefitCodes);
	}
}
