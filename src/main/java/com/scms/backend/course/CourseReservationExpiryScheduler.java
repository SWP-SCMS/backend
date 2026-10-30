package com.scms.backend.course;

import java.time.Clock;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
class CourseReservationExpiryScheduler {

	private static final Logger log = LoggerFactory.getLogger(CourseReservationExpiryScheduler.class);
	private static final int BATCH_SIZE = 100;
	private final CourseReservationExpiryService expiry;
	private final Clock clock;
	private UUID cursor;

	CourseReservationExpiryScheduler(CourseReservationExpiryService expiry, Clock clock) {
		this.expiry = expiry;
		this.clock = clock;
	}

	@Scheduled(fixedDelayString = "${scms.course-reservation-expiry-delay-ms:30000}",
		initialDelayString = "${scms.course-reservation-expiry-initial-delay-ms:30000}")
	void expireAvailable() {
		var now = clock.instant();
		var reservationIds = expiry.findDueReservationIds(now, cursor, BATCH_SIZE);
		if (reservationIds.isEmpty() && cursor != null) {
			cursor = null;
			reservationIds = expiry.findDueReservationIds(now, null, BATCH_SIZE);
		}
		for (UUID reservationId : reservationIds) {
			cursor = reservationId;
			try {
				expiry.expireReservation(reservationId, now);
			}
			catch (RuntimeException exception) {
				log.error("Course reservation expiry failed for reservation {}", reservationId, exception);
			}
		}
	}
}
