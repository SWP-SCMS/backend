package com.scms.backend.notification;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
class NotificationOutboxDeliveryService {

	private final NotificationOutboxRepository outbox;
	private final NotificationProvider provider;
	private final Clock clock;

	NotificationOutboxDeliveryService(NotificationOutboxRepository outbox, NotificationProvider provider,
			Clock clock) {
		this.outbox = outbox;
		this.provider = provider;
		this.clock = clock;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	Optional<NotificationOutboxEvent> claimNext(Instant now, Instant leaseUntil) {
		return outbox.claimNext(now, leaseUntil);
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	void deliver(NotificationOutboxEvent event, Instant now) {
		provider.deliver(event);
		outbox.markSent(event.id(), now);
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	void markFailed(UUID eventId, Instant nextAttemptAt, String error) {
		outbox.markFailed(eventId, nextAttemptAt, clock.instant(), detail(error));
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	void markTerminalFailed(UUID eventId, String error) {
		outbox.markTerminalFailed(eventId, clock.instant(), detail(error));
	}

	private String detail(String error) {
		String detail = error == null ? "Notification delivery failed" : error;
		return detail.substring(0, Math.min(detail.length(), 1000));
	}
}
