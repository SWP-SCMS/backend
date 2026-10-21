package com.scms.backend.membership;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;

import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

class MembershipPlanRepositoryLockContractTests {

	@Test
	void planLockQueryUsesPessimisticWriteOnThePlanRow() throws Exception {
		Method method = MembershipPlanRepository.class.getMethod("findByPlanCodeForUpdate", String.class);

		assertThat(method.getAnnotation(Lock.class).value()).isEqualTo(LockModeType.PESSIMISTIC_WRITE);
		assertThat(method.getAnnotation(Query.class).value())
			.contains("MembershipPlan", "plan.planCode = :code");
	}

	@Test
	void currentPlanStatusLockUsesNativeScalarQueryOutsideThePersistenceContext() {
		Method method = java.util.Arrays.stream(MembershipPlanRepository.class.getMethods())
			.filter(candidate -> candidate.getName().equals("findStatusByPlanCodeForUpdate"))
			.findFirst().orElse(null);

		assertThat(method).isNotNull();
		Query query = method.getAnnotation(Query.class);
		assertThat(query.nativeQuery()).isTrue();
		assertThat(query.value())
			.contains("select status", "membership_plans", "plan_code = :code", "for update");
	}

	@Test
	void sellableOfferLockQueryLocksOfferAndPlanTogether() {
		Method method = java.util.Arrays.stream(MembershipOfferRepository.class.getMethods())
			.filter(candidate -> candidate.getName().equals("findSellablePlanCodeForUpdate"))
			.findFirst().orElse(null);

		assertThat(method).isNotNull();
		Query query = method.getAnnotation(Query.class);
		assertThat(query.nativeQuery()).isTrue();
		assertThat(query.value())
			.contains("membership_offers offer", "join membership_plans plan", "offer.id = :offerId",
				"offer.status = 'ACTIVE'", "plan.status = 'ACTIVE'", "for update of offer, plan");
	}
}
