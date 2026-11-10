package com.scms.backend.membership.plan;

import com.scms.backend.membership.MembershipPlan;
import com.scms.backend.membership.MembershipPlanStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.Set;

public record MembershipPlanResponse(String code, String displayName, MembershipPlanStatus status, Set<String> benefitCodes, @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<MembershipBenefitResponse> benefits, Instant createdAt, Instant updatedAt, Long version, @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean benefitsLocked) {

	static MembershipPlanResponse from(MembershipPlan plan, boolean benefitsLocked) {
		List<MembershipBenefitResponse> benefits = plan.getBenefits().stream().map(MembershipBenefitResponse::from).sorted(MembershipBenefitResponse.displayOrdering()).toList();
		return new MembershipPlanResponse(plan.getPlanCode(), plan.getDisplayName(), plan.getStatus(), benefits.stream().map(MembershipBenefitResponse::code).sorted().collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new)), benefits, plan.getCreatedAt(), plan.getUpdatedAt(), plan.getVersion(), benefitsLocked);
	}
}
