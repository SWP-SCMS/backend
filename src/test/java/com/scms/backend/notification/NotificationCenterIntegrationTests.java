package com.scms.backend.notification;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
class NotificationCenterIntegrationTests {

	@Container
	@ServiceConnection
	static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

	@Autowired AccountRepository accounts;
	@Autowired NotificationRepository notifications;
	@Autowired NotificationService service;

	@Test
	void listsOnlyOwnerNotificationsAndReadAllIsIdempotent() {
		UUID ownerId = UUID.randomUUID();
		UUID otherId = UUID.randomUUID();
		accounts.saveAndFlush(account(ownerId));
		accounts.saveAndFlush(account(otherId));
		notifications.saveAndFlush(new Notification(UUID.randomUUID(), ownerId, "BOOKING_CREATED", "BOOKING",
			UUID.randomUUID(), Map.of("message", "owned")));
		notifications.saveAndFlush(new Notification(UUID.randomUUID(), otherId, "BOOKING_CREATED", "BOOKING",
			UUID.randomUUID(), Map.of("message", "foreign")));

		NotificationPageResponse page = service.list(ownerId, false, PageRequest.of(0, 20));
		assertThat(page.content()).hasSize(1);
		assertThat(page.unreadCount()).isOne();

		assertThat(service.markAllRead(ownerId).updatedCount()).isOne();
		assertThat(service.markAllRead(ownerId).updatedCount()).isZero();
	}

	private Account account(UUID id) {
		return new Account(id, AccountRole.MEMBER, AccountStatus.ACTIVE, "Notification Test", "09" + id.toString()
			.replace("-", "").substring(0, 8), id + "@notification.test", LocalDate.of(1990, 1, 1), "unused");
	}
}
