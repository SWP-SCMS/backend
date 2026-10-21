package com.scms.backend.membership;

import java.util.Comparator;

public record MembershipBenefitResponse(String code, String displayName, String description,
		MembershipBenefitStatus status, int displayOrder, String groupCode, String groupDisplayName,
		Integer groupDisplayOrder) {
	MembershipBenefitResponse(String code, String displayName, String description, MembershipBenefitStatus status) {
		this(code, displayName, description, status, 0, null, null, null);
	}

	static MembershipBenefitResponse from(MembershipBenefit benefit) {
		MembershipBenefitGroup group = benefit.getGroup();
		return new MembershipBenefitResponse(benefit.getCode(), benefit.getDisplayName(), benefit.getDescription(),
			benefit.getStatus(), benefit.getDisplayOrder(), group == null ? null : group.getCode(),
			group == null ? null : group.getDisplayName(), group == null ? null : group.getDisplayOrder());
	}

	static Comparator<MembershipBenefitResponse> displayOrdering() {
		return Comparator.comparingInt((MembershipBenefitResponse benefit) -> benefit.groupDisplayOrder() == null
				? benefit.displayOrder() : benefit.groupDisplayOrder())
			.thenComparingInt(MembershipBenefitResponse::displayOrder)
			.thenComparing(MembershipBenefitResponse::code);
	}
}
