package com.scms.backend.membership;

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
class PaymentNotificationListener {
	private static final Logger logger = LoggerFactory.getLogger(PaymentNotificationListener.class);
	private final NotificationRepository notifications;
	PaymentNotificationListener(NotificationRepository notifications) { this.notifications = notifications; }

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void notifyMember(PaymentCompletedEvent event) {
		try {
			notifications.save(new Notification(UUID.randomUUID(), event.memberAccountId(), "PAYMENT_PAID",
				"PAYMENT", event.paymentId(), Map.of("orderId", event.orderId().toString(), "status", "PAID")));
		} catch (RuntimeException exception) {
			logger.error("Could not create payment notification for {}", event.paymentId(), exception);
		}
	}
}
