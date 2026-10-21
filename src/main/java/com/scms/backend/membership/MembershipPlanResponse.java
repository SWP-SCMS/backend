package com.scms.backend.membership;

import java.time.Instant;
import java.util.Set;

import io.swagger.v3.oas.annotations.media.Schema;

public record MembershipPlanResponse(String code, String displayName, MembershipPlanStatus status,
		Set<String> benefitCodes, Instant createdAt, Instant updatedAt, Long version,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean benefitsLocked) {
	static MembershipPlanResponse from(MembershipPlan plan, boolean benefitsLocked) {
		return new MembershipPlanResponse(plan.getPlanCode(), plan.getDisplayName(), plan.getStatus(),
			plan.getBenefits().stream().map(MembershipBenefit::getCode).sorted().collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new)), plan.getCreatedAt(),
			plan.getUpdatedAt(), plan.getVersion(), benefitsLocked);
	}
}
