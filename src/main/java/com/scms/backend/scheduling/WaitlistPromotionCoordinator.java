package com.scms.backend.scheduling;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class WaitlistPromotionCoordinator {
	private static final Logger log = LoggerFactory.getLogger(WaitlistPromotionCoordinator.class);
	private final ClassSessionRepository sessions;
	private final BookingRepository bookings;
	private final WaitlistPromotionService promotions;
	private final Clock clock;
	private final TransactionTemplate transactions;

	WaitlistPromotionCoordinator(ClassSessionRepository sessions, BookingRepository bookings,
			WaitlistPromotionService promotions, Clock clock, PlatformTransactionManager transactionManager) {
		this.sessions = sessions;
		this.bookings = bookings;
		this.promotions = promotions;
		this.clock = clock;
		this.transactions = new TransactionTemplate(transactionManager);
		this.transactions.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
	}

	public void promote(UUID sessionId) {
		try {
			transactions.executeWithoutResult(ignored -> promoteInTransaction(sessionId));
		}
		catch (RuntimeException exception) {
			log.error("Waitlist promotion failed for session {}", sessionId, exception);
		}
	}

	private void promoteInTransaction(UUID sessionId) {
		ClassSession session = sessions.findByIdForUpdate(sessionId).orElse(null);
		Instant now = clock.instant();
		if (session == null || session.getStatus() != ClassSessionStatus.SCHEDULED
				|| !session.getStartTime().isAfter(now)
				|| bookings.countByClassSessionIdAndStatus(sessionId, BookingStatus.BOOKED) >= session.getCapacity()) return;
		promotions.promoteAvailable(session);
	}
}
