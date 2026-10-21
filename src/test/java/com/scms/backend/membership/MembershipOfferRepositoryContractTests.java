package com.scms.backend.membership;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.EntityGraph;

class MembershipOfferRepositoryContractTests {

	@Test
	void managerListAndDetailQueriesFetchPlanAndBenefitsWithoutStatusFilters() {
		assertManagerReadQuery("findAllWithPlanAndBenefits");
		assertManagerReadQuery("findByIdWithPlanAndBenefits");
	}

	@Test
	void planAndCatalogQueriesFetchBenefitGroups() throws Exception {
		String catalogQuery = MembershipBenefitRepository.class.getMethod("findAllByOrderByCode")
			.getAnnotation(Query.class).value().toLowerCase();
		String plansQuery = MembershipPlanRepository.class.getMethod("findAllWithBenefits")
			.getAnnotation(Query.class).value().toLowerCase();

		assertThat(catalogQuery).contains("join fetch benefit.group");
		assertThat(plansQuery).contains("join fetch benefit.group");
		assertThat(MembershipBenefitRepository.class.getMethod("findAllByCodeIn", Iterable.class)
			.getAnnotation(EntityGraph.class).attributePaths()).containsExactly("group");
	}

	private void assertManagerReadQuery(String methodName) {
		Method method = Arrays.stream(MembershipOfferRepository.class.getMethods())
			.filter(candidate -> candidate.getName().equals(methodName))
			.findFirst().orElse(null);

		assertThat(method).as(methodName).isNotNull();
		String query = method.getAnnotation(Query.class).value().toLowerCase();
		assertThat(query).contains("join fetch offer.plan", "join fetch plan.benefits", "join fetch benefit.group")
			.doesNotContain("offer.status", "plan.status");
	}
}
