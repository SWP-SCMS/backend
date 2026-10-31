package com.scms.backend.course;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
class CourseWithdrawalWaitlistPromotion {
	private final CourseWaitlistService waitlists;
	CourseWithdrawalWaitlistPromotion(CourseWaitlistService waitlists) { this.waitlists = waitlists; }

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	void promote(UUID runId, Instant now) {
		for (int processed = 0; processed < 100 && waitlists.promote(runId, now); processed++) { }
	}
}
