package com.scms.backend.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;

class CoachLeaveLifecycleSchedulerTests {

	@Test
	void startupAndScheduledRunsExpireStartedRequestsUsingServerClock() {
		Instant now = Instant.parse("2026-10-24T01:00:00Z");
		CoachLeaveLifecycleService lifecycle = org.mockito.Mockito.mock(CoachLeaveLifecycleService.class);
		CoachLeaveLifecycleScheduler scheduler = new CoachLeaveLifecycleScheduler(lifecycle,
			Clock.fixed(now, ZoneOffset.UTC));

		scheduler.catchUpAfterStartup();
		scheduler.catchUpOnSchedule();

		verify(lifecycle, times(2)).catchUp(now);
		Scheduled scheduled = org.springframework.util.ReflectionUtils.findMethod(
			CoachLeaveLifecycleScheduler.class, "catchUpOnSchedule").getAnnotation(Scheduled.class);
		assertThat(scheduled.initialDelayString())
			.isEqualTo("${scms.coach-leave-lifecycle-delay-ms:30000}");
	}
}
