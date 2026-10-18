package com.scms.backend.membership;

import java.time.Instant;
import java.util.Set;

public record MembershipPlanResponse(String code, String displayName, MembershipPlanStatus status,
		Set<String> benefitCodes, Instant createdAt, Instant updatedAt, Long version) {
	static MembershipPlanResponse from(MembershipPlan plan) {
		return new MembershipPlanResponse(plan.getPlanCode(), plan.getDisplayName(), plan.getStatus(),
			plan.getBenefits().stream().map(MembershipBenefit::getCode).sorted().collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new)), plan.getCreatedAt(),
			plan.getUpdatedAt(), plan.getVersion());
	}
}
