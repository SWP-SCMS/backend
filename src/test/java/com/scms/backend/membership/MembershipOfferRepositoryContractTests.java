package com.scms.backend.membership;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;

class MembershipOfferRepositoryContractTests {

	@Test
	void managerListAndDetailQueriesFetchPlanAndBenefitsWithoutStatusFilters() {
		assertManagerReadQuery("findAllWithPlanAndBenefits");
		assertManagerReadQuery("findByIdWithPlanAndBenefits");
	}

	private void assertManagerReadQuery(String methodName) {
		Method method = Arrays.stream(MembershipOfferRepository.class.getMethods())
			.filter(candidate -> candidate.getName().equals(methodName))
			.findFirst().orElse(null);

		assertThat(method).as(methodName).isNotNull();
		String query = method.getAnnotation(Query.class).value().toLowerCase();
		assertThat(query).contains("join fetch offer.plan", "join fetch plan.benefits")
			.doesNotContain("offer.status", "plan.status");
	}
}
