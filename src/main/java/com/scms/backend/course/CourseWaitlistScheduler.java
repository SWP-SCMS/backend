package com.scms.backend.course;

import java.time.Clock;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
class CourseWaitlistScheduler {
	private static final Logger log = LoggerFactory.getLogger(CourseWaitlistScheduler.class);
	private final CourseWaitlistService service;
	private final Clock clock;
	private final AtomicReference<UUID> cursor = new AtomicReference<>();
	CourseWaitlistScheduler(CourseWaitlistService service, Clock clock) { this.service = service; this.clock = clock; }

	@Scheduled(fixedDelayString = "${scms.course-waitlist-delay-ms:30000}",
		initialDelayString = "${scms.course-waitlist-initial-delay-ms:30000}")
	void promoteAvailable() {
		var now = clock.instant();
		var runIds = service.candidateRunIds(now, cursor.get());
		if (runIds.isEmpty()) {
			cursor.set(null);
			return;
		}
		for (var runId : runIds) {
			try {
				for (int processed = 0; processed < 100 && service.promote(runId, now); processed++) { }
			}
			catch (RuntimeException exception) { log.error("Course waitlist promotion failed for run {}", runId, exception); }
		}
		cursor.set(runIds.getLast());
	}
}
