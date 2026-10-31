package com.scms.backend.course;

import java.time.Clock;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
class CourseTransferExpiryScheduler {
	private final CourseTransferService transfers;
	private final Clock clock;
	CourseTransferExpiryScheduler(CourseTransferService transfers, Clock clock) { this.transfers = transfers; this.clock = clock; }
	@Scheduled(fixedDelayString = "${scms.course.transfer-expiry-delay-ms:60000}")
	void expire() { transfers.expireDue(clock.instant()); }
}
