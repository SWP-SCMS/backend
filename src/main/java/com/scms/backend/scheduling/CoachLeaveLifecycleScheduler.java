package com.scms.backend.scheduling;

import java.time.Clock;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
class CoachLeaveLifecycleScheduler {

	private final CoachLeaveLifecycleService lifecycle;
	private final Clock clock;

	CoachLeaveLifecycleScheduler(CoachLeaveLifecycleService lifecycle, Clock clock) {
		this.lifecycle = lifecycle;
		this.clock = clock;
	}

	@EventListener(ApplicationReadyEvent.class)
	void catchUpAfterStartup() {
		lifecycle.catchUp(clock.instant());
	}

	@Scheduled(fixedDelayString = "${scms.coach-leave-lifecycle-delay-ms:30000}",
		initialDelayString = "${scms.coach-leave-lifecycle-delay-ms:30000}")
	void catchUpOnSchedule() {
		lifecycle.catchUp(clock.instant());
	}
}
