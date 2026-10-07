package com.scms.backend.scheduling;

import java.time.Clock;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
class SessionLifecycleScheduler {

	private final SessionLifecycleService lifecycle;
	private final Clock clock;

	SessionLifecycleScheduler(SessionLifecycleService lifecycle, Clock clock) {
		this.lifecycle = lifecycle;
		this.clock = clock;
	}

	@EventListener(ApplicationReadyEvent.class)
	void catchUpAfterStartup() {
		lifecycle.catchUp(clock.instant());
	}

	@Scheduled(fixedDelayString = "${scms.session-lifecycle-delay-ms:30000}")
	void catchUpOnSchedule() {
		lifecycle.catchUp(clock.instant());
	}
}
