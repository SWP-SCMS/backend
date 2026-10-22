package com.scms.backend.membership;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;

class MembershipRepositoryQueryTests {

	@Test
	void lifecycleDueMemberQueryIncludesReservedMembershipsAfterExpiredMemberships() throws Exception {
		Method method = MembershipRepository.class.getMethod("findDueMemberAccountIds", Instant.class, java.util.UUID.class);
		String query = method.getAnnotation(Query.class).value();

		assertThat(query).contains("status = 'RESERVED'")
			.contains("status = 'EXPIRED'")
			.contains("exists")
			.contains("status = 'ACTIVE'");
		assertThat(query).contains("not exists");
		assertThat(query).contains("afterMemberId");
		assertThat(query).contains("cast(:afterMemberId as uuid) is null");
	}
}
