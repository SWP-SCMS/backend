package com.scms.backend.course;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class CourseReservationExpirySchedulerTests {

	private static final Instant NOW = Instant.parse("2026-11-01T00:00:00Z");

	@Test
	void failureDoesNotStarveLaterReservationsAndCursorRecovers() {
		CourseReservationExpiryService expiry = mock(CourseReservationExpiryService.class);
		UUID failed = new UUID(0, 1);
		UUID later = new UUID(0, 2);
		when(expiry.findDueReservationIds(NOW, null, 100)).thenReturn(List.of(failed, later));
		when(expiry.findDueReservationIds(NOW, later, 100)).thenReturn(List.of());
		doThrow(new IllegalStateException("forced failure")).when(expiry).expireReservation(failed, NOW);
		CourseReservationExpiryScheduler scheduler = new CourseReservationExpiryScheduler(expiry,
			Clock.fixed(NOW, ZoneOffset.UTC));

		scheduler.expireAvailable();
		scheduler.expireAvailable();

		var order = inOrder(expiry);
		order.verify(expiry).findDueReservationIds(NOW, null, 100);
		order.verify(expiry).expireReservation(failed, NOW);
		order.verify(expiry).expireReservation(later, NOW);
		order.verify(expiry).findDueReservationIds(NOW, later, 100);
		order.verify(expiry).findDueReservationIds(NOW, null, 100);
	}
}
