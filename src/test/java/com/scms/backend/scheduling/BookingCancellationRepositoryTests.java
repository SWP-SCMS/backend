package com.scms.backend.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.Test;

class BookingCancellationRepositoryTests {
	@Test
	void returningCancellationPreservesFlushAndClearSemantics() {
		EntityManager entityManager = mock(EntityManager.class);
		Query query = mock(Query.class);
		UUID memberId = UUID.randomUUID();
		UUID actorId = UUID.randomUUID();
		UUID sessionId = UUID.randomUUID();
		Instant now = Instant.parse("2026-10-24T03:00:00Z");
		when(entityManager.createNativeQuery(anyString(), eq(UUID.class))).thenReturn(query);
		when(query.setParameter(anyString(), org.mockito.ArgumentMatchers.any())).thenReturn(query);
		when(query.getResultList()).thenReturn(List.of(sessionId));

		List<UUID> changed = new BookingCancellationRepositoryImpl(entityManager)
			.cancelFutureBookingsForMembershipTermination(memberId, actorId, now);

		assertThat(changed).containsExactly(sessionId);
		var order = inOrder(entityManager, query);
		order.verify(entityManager).flush();
		order.verify(entityManager).createNativeQuery(anyString(), eq(UUID.class));
		order.verify(query).getResultList();
		order.verify(entityManager).clear();
	}
}
