package com.scms.backend.membership;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigInteger;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class MembershipOfferAdminResponseTests {

	@Test
	void mutationResponseKeepsExistingShape() {
		MembershipPlan plan = new MembershipPlan("BASIC", "Basic", MembershipPlanStatus.ACTIVE, Set.of());
		MembershipOffer offer = new MembershipOffer(UUID.randomUUID(), plan, "Offer", "Description",
			BigInteger.valueOf(500_000), "VND", 30, MembershipOfferStatus.ACTIVE, UUID.randomUUID());

		JsonNode response = new ObjectMapper().valueToTree(MembershipOfferAdminResponse.from(offer));

		assertThat(response.fieldNames()).toIterable().containsExactlyInAnyOrder(
			"offerId", "planCode", "name", "description", "priceAmount", "currencyCode", "durationDays", "status");
		assertThat(response.has("planDisplayName")).isFalse();
		assertThat(response.has("benefitCodes")).isFalse();
		assertThat(response.has("benefits")).isFalse();
	}

	@Test
	void exposesPlanDisplayNameAndBenefitsSortedByCode() {
		MembershipBenefit zeta = new MembershipBenefit("Z_ACCESS", "Zeta", "Z access",
			MembershipBenefitStatus.INACTIVE);
		MembershipBenefit alpha = new MembershipBenefit("A_ACCESS", "Alpha", "A access",
			MembershipBenefitStatus.ACTIVE);
		MembershipPlan plan = new MembershipPlan("ARCHIVED", "Archived plan", MembershipPlanStatus.INACTIVE,
			Set.of(zeta, alpha));
		MembershipOffer offer = new MembershipOffer(UUID.randomUUID(), plan, "Archived offer", "Still manageable",
			BigInteger.valueOf(500_000), "VND", 30, MembershipOfferStatus.INACTIVE, UUID.randomUUID());

		JsonNode response = new ObjectMapper().valueToTree(MembershipOfferAdminDetailResponse.from(offer));

		assertThat(response.at("/planDisplayName").asText()).isEqualTo("Archived plan");
		assertThat(response.at("/benefitCodes/0").asText()).isEqualTo("A_ACCESS");
		assertThat(response.at("/benefitCodes/1").asText()).isEqualTo("Z_ACCESS");
		assertThat(response.at("/benefits/0/code").asText()).isEqualTo("A_ACCESS");
		assertThat(response.at("/benefits/1/code").asText()).isEqualTo("Z_ACCESS");
		assertThat(response.at("/benefits/1/status").asText()).isEqualTo("INACTIVE");
		assertThat(response.at("/status").asText()).isEqualTo("INACTIVE");
	}
}
