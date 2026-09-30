package com.scms.backend.notification;

import java.util.Map;
import java.util.UUID;

import com.scms.backend.reception.MemberCreatedEvent;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MemberCreatedNotificationWriter {

	private static final String ACCOUNT_CREATED = "ACCOUNT_CREATED";
	private static final String ACCOUNT = "ACCOUNT";

	private final NotificationRepository repository;

	MemberCreatedNotificationWriter(NotificationRepository repository) {
		this.repository = repository;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void write(MemberCreatedEvent event) {
		repository.saveAndFlush(new Notification(UUID.randomUUID(), event.accountId(), ACCOUNT_CREATED, ACCOUNT,
			event.accountId(), Map.of("memberId", event.memberId())));
	}
}
