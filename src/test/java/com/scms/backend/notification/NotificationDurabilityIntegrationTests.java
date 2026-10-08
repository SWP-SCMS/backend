package com.scms.backend.notification;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
class NotificationDurabilityIntegrationTests {

	@Container
	@ServiceConnection
	static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

	@Autowired NotificationWriter writer;
	@Autowired AccountRepository accounts;
	@Autowired JdbcTemplate db;
	@Autowired PlatformTransactionManager transactionManager;

	@Test
	void repeatedAndConcurrentEventKeyCreatesOnePendingNotification() throws Exception {
		UUID recipientId = UUID.randomUUID();
		accounts.saveAndFlush(new Account(recipientId, AccountRole.MEMBER, AccountStatus.ACTIVE, "Inbox Member",
			"09" + recipientId.toString().replace("-", "").substring(0, 8), recipientId + "@example.test",
			LocalDate.of(1990, 1, 1), "unused"));
		TransactionTemplate transactions = new TransactionTemplate(transactionManager);
		String repeatedKey = "ACCOUNT_CREATED:" + recipientId;

		transactions.executeWithoutResult(status -> write(repeatedKey, recipientId));
		transactions.executeWithoutResult(status -> write(repeatedKey, recipientId));
		assertThat(count(repeatedKey)).isOne();

		String concurrentKey = "BOOKING_CREATED:" + UUID.randomUUID();
		CountDownLatch ready = new CountDownLatch(4);
		CountDownLatch start = new CountDownLatch(1);
		try (var executor = Executors.newFixedThreadPool(4)) {
			List<java.util.concurrent.Future<?>> futures = new ArrayList<>();
			for (int index = 0; index < 4; index++) {
				futures.add(executor.submit(() -> {
					ready.countDown();
					start.await();
					transactions.executeWithoutResult(status -> write(concurrentKey, recipientId));
					return null;
				}));
			}
			ready.await();
			start.countDown();
			for (var future : futures) future.get();
		}

		assertThat(count(concurrentKey)).isOne();
		assertThat(db.queryForObject("select delivery_status from notifications where event_key=?", String.class,
			concurrentKey)).isEqualTo("PENDING");
	}

	private void write(String eventKey, UUID recipientId) {
		writer.write(eventKey, recipientId, "ACCOUNT_CREATED", "ACCOUNT", recipientId, Map.of());
	}

	private long count(String eventKey) {
		return db.queryForObject("select count(*) from notifications where event_key=?", Long.class, eventKey);
	}
}
