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
import org.mockito.InOrder;

class CourseRunClosureSchedulerTests {
	private static final Instant NOW = Instant.parse("2026-10-31T00:00:00Z");

	@Test
	void failedRunDoesNotStarveLaterRunsAndCursorRecovers() {
		CourseRunClosureService closure = mock(CourseRunClosureService.class);
		UUID failed = new UUID(0, 1);
		UUID later = new UUID(0, 2);
		when(closure.findDueRunIds(NOW, null, 100)).thenReturn(List.of(failed, later));
		when(closure.findDueRunIds(NOW, later, 100)).thenReturn(List.of());
		doThrow(new IllegalStateException("forced failure")).when(closure).closeAtRegistration(failed, NOW);
		CourseRunClosureScheduler scheduler = new CourseRunClosureScheduler(closure,
			Clock.fixed(NOW, ZoneOffset.UTC));

		scheduler.closeDueRuns();
		scheduler.closeDueRuns();

		InOrder order = inOrder(closure);
		order.verify(closure).findDueRunIds(NOW, null, 100);
		order.verify(closure).closeAtRegistration(failed, NOW);
		order.verify(closure).closeAtRegistration(later, NOW);
		order.verify(closure).findDueRunIds(NOW, later, 100);
		order.verify(closure).findDueRunIds(NOW, null, 100);
	}
}
