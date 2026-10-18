package com.scms.backend.membership;

public record MembershipBenefitResponse(String code, String displayName, String description,
		MembershipBenefitStatus status) {
	static MembershipBenefitResponse from(MembershipBenefit benefit) {
		return new MembershipBenefitResponse(benefit.getCode(), benefit.getDisplayName(), benefit.getDescription(),
			benefit.getStatus());
	}
}
