package com.scms.backend.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.annotation.Scheduled;

@ExtendWith(MockitoExtension.class)
class SessionLifecycleServiceTests {

	@Mock ClassSessionRepository sessions;
	@Mock AttendanceRepository attendance;

	@Test
	void catchUpAdvancesStartedSessionsBackfillsAttendanceAndCompletesEndedSessions() {
		Instant now = Instant.parse("2026-10-08T02:00:00Z");
		SessionLifecycleService service = new SessionLifecycleService(sessions, attendance);

		service.catchUp(now);

		InOrder writes = inOrder(sessions, attendance);
		writes.verify(sessions).advanceScheduledToInProgress(now);
		writes.verify(attendance).insertAbsentForStartedBookings(now);
		writes.verify(sessions).advanceInProgressToCompleted(now);
	}

	@Test
	void startupAndScheduledRunsUseTheServerClock() {
		Instant now = Instant.parse("2026-10-08T02:00:00Z");
		SessionLifecycleService service = org.mockito.Mockito.mock(SessionLifecycleService.class);
		SessionLifecycleScheduler scheduler = new SessionLifecycleScheduler(service,
			Clock.fixed(now, ZoneOffset.UTC));

		scheduler.catchUpAfterStartup();
		scheduler.catchUpOnSchedule();

		verify(service, times(2)).catchUp(now);
		Scheduled scheduled = org.springframework.util.ReflectionUtils.findMethod(
			SessionLifecycleScheduler.class, "catchUpOnSchedule").getAnnotation(Scheduled.class);
		assertThat(scheduled.initialDelayString()).isEqualTo("${scms.session-lifecycle-delay-ms:30000}");
	}
}
