package com.scms.backend.scheduling;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

class WaitlistPromotionSchedulerTests {
	private static final Instant NOW = Instant.parse("2026-10-24T03:00:00Z");

	@Test
	void failureDoesNotStarveLaterSessionsAndCursorAdvancesPastIt() {
		ClassSessionRepository sessions = mock(ClassSessionRepository.class);
		WaitlistPromotionCoordinator promotions = mock(WaitlistPromotionCoordinator.class);
		UUID failed = new UUID(0, 1);
		UUID later = new UUID(0, 2);
		when(sessions.findWaitlistPromotionCandidateIds(NOW, null, PageRequest.of(0, 100)))
			.thenReturn(List.of(failed, later), List.of(failed, later));
		when(sessions.findWaitlistPromotionCandidateIds(NOW, later, PageRequest.of(0, 100)))
			.thenReturn(List.of());
		doThrow(new IllegalStateException("forced failure")).when(promotions).promote(failed);
		WaitlistPromotionScheduler scheduler = new WaitlistPromotionScheduler(sessions, promotions,
			Clock.fixed(NOW, ZoneOffset.UTC));

		scheduler.promoteAvailable();
		scheduler.promoteAvailable();

		var order = inOrder(promotions, sessions);
		order.verify(promotions).promote(failed);
		order.verify(promotions).promote(later);
		order.verify(sessions).findWaitlistPromotionCandidateIds(NOW, later, PageRequest.of(0, 100));
		verify(promotions, times(2)).promote(failed);
		verify(promotions, times(2)).promote(later);
	}
}
