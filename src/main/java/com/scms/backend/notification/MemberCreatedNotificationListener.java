package com.scms.backend.notification;

import com.scms.backend.reception.MemberCreatedEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class MemberCreatedNotificationListener {

	private static final Logger logger = LoggerFactory.getLogger(MemberCreatedNotificationListener.class);

	private final MemberCreatedNotificationWriter writer;

	MemberCreatedNotificationListener(MemberCreatedNotificationWriter writer) {
		this.writer = writer;
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onMemberCreated(MemberCreatedEvent event) {
		try {
			writer.write(event);
		}
		catch (RuntimeException exception) {
			logger.error("Could not persist ACCOUNT_CREATED notification for account {}", event.accountId(),
				exception);
		}
	}
}
