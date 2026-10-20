package com.scms.backend.membership;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigInteger;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class MembershipOfferResponseTests {

	@Test
	void exposesSortedBenefitsWithoutChangingLegacyCapabilities() {
		MembershipBenefit coaching = new MembershipBenefit("PERSONAL_COACHING", "Coaching", "Coach access",
			MembershipBenefitStatus.ACTIVE);
		MembershipBenefit booking = new MembershipBenefit("CLASS_BOOKING", "Booking", "Class access",
			MembershipBenefitStatus.ACTIVE);
		MembershipPlan plan = new MembershipPlan("PLUS", "Plus", MembershipPlanStatus.ACTIVE,
			Set.of(coaching, booking));
		MembershipOffer offer = new MembershipOffer(UUID.randomUUID(), plan, "Plus 30", "Plus", 
			BigInteger.valueOf(500_000), "VND", 30, MembershipOfferStatus.ACTIVE, UUID.randomUUID());

		JsonNode response = new ObjectMapper().valueToTree(MembershipOfferResponse.from(offer));

		assertThat(response.at("/benefitCodes/0").asText()).isEqualTo("CLASS_BOOKING");
		assertThat(response.at("/benefitCodes/1").asText()).isEqualTo("PERSONAL_COACHING");
		assertThat(response.at("/benefits/0/code").asText()).isEqualTo("CLASS_BOOKING");
		assertThat(response.at("/benefits/1/code").asText()).isEqualTo("PERSONAL_COACHING");
		assertThat(response.get("supportsBooking").asBoolean()).isTrue();
		assertThat(response.get("supportsPersonalCoaching").asBoolean()).isTrue();
	}
}
