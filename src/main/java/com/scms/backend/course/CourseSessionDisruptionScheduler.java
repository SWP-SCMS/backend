package com.scms.backend.course;

import java.time.Clock;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
class CourseSessionDisruptionScheduler {
	private static final Logger log = LoggerFactory.getLogger(CourseSessionDisruptionScheduler.class);
	private final CourseSessionDisruptionService service;
	private final Clock clock;

	CourseSessionDisruptionScheduler(CourseSessionDisruptionService service, Clock clock) {
		this.service = service;
		this.clock = clock;
	}

	@Scheduled(fixedDelayString = "${scms.course-session-disruption-delay-ms:30000}",
		initialDelayString = "${scms.course-session-disruption-initial-delay-ms:30000}")
	void advanceDueSessions() {
		try {
			for (UUID sessionId : service.advance(clock.instant())) {
				log.warn("Course session has remained without a makeup for 30 days: sessionId={}", sessionId);
			}
		}
		catch (RuntimeException exception) {
			log.error("Course session disruption advancement failed", exception);
		}
	}
}
