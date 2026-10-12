package com.scms.backend.notification;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.auth.InvalidAuthenticatedAccountException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

	private final NotificationRepository repository;
	private final AccountRepository accounts;
	private final Clock clock;

	NotificationService(NotificationRepository repository, AccountRepository accounts, Clock clock) {
		this.repository = repository;
		this.accounts = accounts;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	NotificationPageResponse list(UUID accountId, boolean unreadOnly, Pageable pageable) {
		ensureActive(accountId);
		Page<Notification> page = unreadOnly
			? repository.findByRecipientAccountIdAndReadAtIsNull(accountId, pageable)
			: repository.findByRecipientAccountId(accountId, pageable);
		return new NotificationPageResponse(page.getContent().stream().map(NotificationResponse::from).toList(),
			page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages(),
			repository.countByRecipientAccountIdAndReadAtIsNull(accountId));
	}

	@Transactional
	NotificationReadResponse markRead(UUID accountId, UUID notificationId) {
		ensureActive(accountId);
		if (repository.findByIdAndRecipientAccountId(notificationId, accountId).isEmpty()) {
			throw new NotificationNotFoundException();
		}
		int updated = repository.markRead(notificationId, accountId, Instant.now(clock));
		return new NotificationReadResponse(updated, repository.countByRecipientAccountIdAndReadAtIsNull(accountId));
	}

	@Transactional
	NotificationReadResponse markAllRead(UUID accountId) {
		ensureActive(accountId);
		int updated = repository.markAllRead(accountId, Instant.now(clock));
		return new NotificationReadResponse(updated, repository.countByRecipientAccountIdAndReadAtIsNull(accountId));
	}

	private void ensureActive(UUID accountId) {
		if (!accounts.existsByIdAndStatus(accountId, AccountStatus.ACTIVE)) {
			throw new InvalidAuthenticatedAccountException();
		}
	}
}
