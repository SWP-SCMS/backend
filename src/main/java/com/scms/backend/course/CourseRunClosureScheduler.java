package com.scms.backend.course;

import java.time.Clock;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
class CourseRunClosureScheduler {
	private static final Logger log = LoggerFactory.getLogger(CourseRunClosureScheduler.class);
	private static final int BATCH_SIZE = 100;
	private final CourseRunClosureService closure;
	private final Clock clock;
	private UUID cursor;

	CourseRunClosureScheduler(CourseRunClosureService closure, Clock clock) {
		this.closure = closure;
		this.clock = clock;
	}

	@Scheduled(fixedDelayString = "${scms.course-run-closure-delay-ms:30000}",
		initialDelayString = "${scms.course-run-closure-initial-delay-ms:30000}")
	void closeDueRuns() {
		var now = clock.instant();
		var runIds = closure.findDueRunIds(now, cursor, BATCH_SIZE);
		if (runIds.isEmpty() && cursor != null) {
			cursor = null;
			runIds = closure.findDueRunIds(now, null, BATCH_SIZE);
		}
		for (UUID runId : runIds) {
			cursor = runId;
			try {
				closure.closeAtRegistration(runId, now);
			}
			catch (RuntimeException exception) {
				log.error("Course run closure failed for run {}", runId, exception);
			}
		}
	}
}
