package com.scms.backend.scheduling;

import java.util.Map;
import java.util.UUID;

import com.scms.backend.notification.Notification;
import com.scms.backend.notification.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class BookingCreatedNotificationListener {

	private static final Logger logger = LoggerFactory.getLogger(BookingCreatedNotificationListener.class);
	private final NotificationRepository notifications;

	BookingCreatedNotificationListener(NotificationRepository notifications) {
		this.notifications = notifications;
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void notifyMember(BookingCreatedEvent event) {
		try {
			notifications.save(new Notification(UUID.randomUUID(), event.memberId(), "BOOKING_CREATED", "BOOKING",
				event.bookingId(), Map.of("sessionId", event.sessionId().toString())));
		}
		catch (RuntimeException exception) {
			logger.error("Could not create Booking notification for {}", event.bookingId(), exception);
		}
	}
}
