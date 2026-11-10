package com.scms.backend.membership.plan;

import com.scms.backend.membership.MembershipPlanStatus;
import java.util.List;

public record MembershipPlanPatchRequest(String code, String displayName, MembershipPlanStatus status, List<String> benefitCodes) {

	public MembershipPlanPatchRequest(String displayName, MembershipPlanStatus status, List<String> benefitCodes) {
		this(null, displayName, status, benefitCodes);
	}
}
