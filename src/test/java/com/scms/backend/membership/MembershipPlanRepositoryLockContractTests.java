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
}
