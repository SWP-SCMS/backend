package com.scms.backend.membership;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigInteger;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class MembershipOfferResponseCompatibilityTests {

	@Test
	void preservesMembershipPlanCodeAtThePublicOfferResponseBoundary() {
		MembershipPlan plan = mock(MembershipPlan.class);
		when(plan.getPlanCode()).thenReturn(MembershipPlanCode.BASIC.name());
		when(plan.getDisplayName()).thenReturn("Basic");
		MembershipOffer offer = new MembershipOffer(UUID.randomUUID(), plan, "Basic 30", "Access",
			BigInteger.valueOf(500000), "VND", 30, MembershipOfferStatus.ACTIVE, UUID.randomUUID());

		MembershipOfferResponse response = MembershipOfferResponse.from(offer);

		assertThat(response.planCode()).isEqualTo(MembershipPlanCode.BASIC);
	}
}
