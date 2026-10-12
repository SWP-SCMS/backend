package com.scms.backend.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import com.scms.backend.account.AccountRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

class NotificationServiceTests {

	@Test
	void listsOnlyOwnerNotificationsWithUnreadCount() {
		NotificationRepository repository = Mockito.mock(NotificationRepository.class);
		AccountRepository accounts = Mockito.mock(AccountRepository.class);
		NotificationService service = new NotificationService(repository, accounts,
			Clock.fixed(Instant.parse("2026-10-12T00:00:00Z"), ZoneOffset.UTC));
		UUID accountId = UUID.randomUUID();
		when(accounts.existsByIdAndStatus(eq(accountId), any())).thenReturn(true);
		Notification notification = new Notification(UUID.randomUUID(), accountId, "BOOKING_CREATED", "BOOKING",
			UUID.randomUUID(), java.util.Map.of("sessionId", "s1"));
		when(repository.findByRecipientAccountId(eq(accountId), any())).thenReturn(new PageImpl<>(List.of(notification),
			PageRequest.of(0, 20), 1));
		when(repository.countByRecipientAccountIdAndReadAtIsNull(accountId)).thenReturn(1L);

		NotificationPageResponse result = service.list(accountId, false, PageRequest.of(0, 20));

		assertThat(result.content()).hasSize(1);
		assertThat(result.unreadCount()).isOne();
		verify(repository).findByRecipientAccountId(eq(accountId), any());
	}

	@Test
	void foreignNotificationIsNotFoundAndRepeatedReadIsIdempotent() {
		NotificationRepository repository = Mockito.mock(NotificationRepository.class);
		AccountRepository accounts = Mockito.mock(AccountRepository.class);
		NotificationService service = new NotificationService(repository, accounts, Clock.systemUTC());
		UUID accountId = UUID.randomUUID();
		UUID notificationId = UUID.randomUUID();
		when(accounts.existsByIdAndStatus(eq(accountId), any())).thenReturn(true);
		when(repository.findByIdAndRecipientAccountId(notificationId, accountId)).thenReturn(java.util.Optional.empty());

		org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.markRead(accountId, notificationId))
			.isInstanceOf(NotificationNotFoundException.class);
	}
}
