package com.scms.backend.membership;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class MembershipBenefitResponseGroupTests {

	@Test
	void ordersIndependentBenefitsBeforeGroupedBenefitsAndChildrenByDisplayOrder() {
		MembershipBenefitResponse trackingSecond = benefit("TRAINING_FEEDBACK", 20,
			"TRAINING_TRACKING", "Theo dõi luyện tập", 70);
		MembershipBenefitResponse independent = benefit("CENTER_ACCESS", 10, null, null, null);
		MembershipBenefitResponse personalSecond = benefit("TRAINING_PLAN", 20,
			"PERSONAL_TRAINING", "Huấn luyện cá nhân", 60);
		MembershipBenefitResponse personalFirst = benefit("PERSONAL_COACHING", 10,
			"PERSONAL_TRAINING", "Huấn luyện cá nhân", 60);

		assertThat(List.of(trackingSecond, personalSecond, independent, personalFirst).stream()
			.sorted(MembershipBenefitResponse.displayOrdering()).map(MembershipBenefitResponse::code))
			.containsExactly("CENTER_ACCESS", "PERSONAL_COACHING", "TRAINING_PLAN", "TRAINING_FEEDBACK");
	}

	@Test
	void independentBenefitHasNoGroupMetadata() {
		MembershipBenefitResponse response = benefit("CENTER_ACCESS", 10, null, null, null);

		assertThat(response.groupCode()).isNull();
		assertThat(response.groupDisplayName()).isNull();
		assertThat(response.groupDisplayOrder()).isNull();
	}

	private MembershipBenefitResponse benefit(String code, int displayOrder, String groupCode,
			String groupDisplayName, Integer groupDisplayOrder) {
		return new MembershipBenefitResponse(code, code, code, MembershipBenefitStatus.ACTIVE,
			displayOrder, groupCode, groupDisplayName, groupDisplayOrder);
	}
}
