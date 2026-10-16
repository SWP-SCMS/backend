package com.scms.backend.notification;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class NotificationOutboxDispatcher {

	private static final Logger log = LoggerFactory.getLogger(NotificationOutboxDispatcher.class);
	private static final int BATCH_SIZE = 100;
	private static final int MAX_ATTEMPTS = 10;
	private final NotificationOutboxDeliveryService delivery;
	private final Clock clock;

	NotificationOutboxDispatcher(NotificationOutboxDeliveryService delivery, Clock clock) {
		this.delivery = delivery;
		this.clock = clock;
	}

	@Scheduled(fixedDelayString = "${scms.notification-outbox-delay-ms:10000}")
	public void dispatchAvailable() {
		for (int index = 0; index < BATCH_SIZE; index++) {
			Instant now = clock.instant();
			Optional<NotificationOutboxEvent> claimed;
			try {
				claimed = delivery.claimNext(now, now.plusSeconds(60));
			}
			catch (RuntimeException exception) {
				log.warn("Notification outbox claim failed", exception);
				return;
			}
			if (claimed.isEmpty()) return;
			NotificationOutboxEvent event = claimed.orElseThrow();
			try {
				delivery.deliver(event, now);
			}
			catch (RuntimeException exception) {
				try {
					if (event.attemptCount() >= MAX_ATTEMPTS) {
						delivery.markTerminalFailed(event.id(), exception.getMessage());
					} else {
						long delaySeconds = Math.min(1800,
							30L << Math.min(Math.max(event.attemptCount() - 1, 0), 6));
						delivery.markFailed(event.id(), now.plusSeconds(delaySeconds), exception.getMessage());
					}
				}
				catch (RuntimeException retryException) {
					log.warn("Notification outbox retry scheduling failed for event {}", event.eventKey(), retryException);
				}
				log.warn("Notification outbox delivery failed for event {}", event.eventKey(), exception);
			}
		}
	}
}
