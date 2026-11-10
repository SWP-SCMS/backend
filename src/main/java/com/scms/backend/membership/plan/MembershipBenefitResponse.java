package com.scms.backend.membership.plan;

import com.scms.backend.membership.MembershipBenefit;
import com.scms.backend.membership.MembershipBenefitGroup;
import com.scms.backend.membership.MembershipBenefitStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Comparator;

public record MembershipBenefitResponse(String code, String displayName, String description, MembershipBenefitStatus status, @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0") int displayOrder, @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String groupCode, @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String groupDisplayName, @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true, minimum = "0") Integer groupDisplayOrder) {

	public MembershipBenefitResponse(String code, String displayName, String description, MembershipBenefitStatus status) {
		this(code, displayName, description, status, 0, null, null, null);
	}

	public static MembershipBenefitResponse from(MembershipBenefit benefit) {
		MembershipBenefitGroup group = benefit.getGroup();
		return new MembershipBenefitResponse(benefit.getCode(), benefit.getDisplayName(), benefit.getDescription(), benefit.getStatus(), benefit.getDisplayOrder(), group == null ? null : group.getCode(), group == null ? null : group.getDisplayName(), group == null ? null : group.getDisplayOrder());
	}

	public static Comparator<MembershipBenefitResponse> displayOrdering() {
		return Comparator.comparingInt((MembershipBenefitResponse benefit) -> benefit.groupDisplayOrder() == null ? benefit.displayOrder() : benefit.groupDisplayOrder()).thenComparingInt(MembershipBenefitResponse::displayOrder).thenComparing(MembershipBenefitResponse::code);
	}
}
