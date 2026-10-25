package com.scms.backend.scheduling;

import java.time.Clock;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
class WaitlistPromotionScheduler {
	private static final Logger log = LoggerFactory.getLogger(WaitlistPromotionScheduler.class);
	private static final int BATCH_SIZE = 100;
	private final ClassSessionRepository sessions;
	private final WaitlistPromotionCoordinator promotions;
	private final Clock clock;
	private UUID cursor;

	WaitlistPromotionScheduler(ClassSessionRepository sessions, WaitlistPromotionCoordinator promotions, Clock clock) {
		this.sessions = sessions;
		this.promotions = promotions;
		this.clock = clock;
	}

	@Scheduled(fixedDelayString = "${scms.waitlist-promotion-delay-ms:30000}",
		initialDelayString = "${scms.waitlist-promotion-delay-ms:30000}")
	void promoteAvailable() {
		var now = clock.instant();
		var sessionIds = sessions.findWaitlistPromotionCandidateIds(now, cursor,
			PageRequest.of(0, BATCH_SIZE));
		if (sessionIds.isEmpty() && cursor != null) {
			cursor = null;
			sessionIds = sessions.findWaitlistPromotionCandidateIds(now, null, PageRequest.of(0, BATCH_SIZE));
		}
		if (sessionIds.isEmpty()) return;
		for (UUID sessionId : sessionIds) {
			cursor = sessionId;
			try {
				promotions.promote(sessionId);
			}
			catch (RuntimeException exception) {
				log.error("Waitlist promotion scheduling failed for session {}", sessionId, exception);
			}
		}
	}
}
