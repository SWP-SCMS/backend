package com.scms.backend.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.notification.NotificationWriter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
class CoachLeaveExpiryIntegrationTests {

	@Container
	@ServiceConnection
	static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

	@Autowired CoachLeaveService service;
	@Autowired CoachLeaveLifecycleService lifecycle;
	@Autowired CoachLeaveRepository leaves;
	@Autowired AccountRepository accounts;
	@Autowired JdbcTemplate db;
	@MockitoBean NotificationWriter notifications;

	@Test
	void approvalAndLifecycleRaceAlwaysEndsExpiredWithoutApprovalNotification() throws Exception {
		Account manager = saveAccount(AccountRole.MANAGER, "Manager");
		Account coach = saveAccount(AccountRole.COACH, "Coach");
		Instant start = Instant.parse("2020-01-01T02:00:00Z");
		CoachLeaveRequest leave = leaves.saveAndFlush(new CoachLeaveRequest(UUID.randomUUID(), coach, start,
			start.plusSeconds(3600), "Historical pending leave"));
		CountDownLatch ready = new CountDownLatch(2);
		CountDownLatch go = new CountDownLatch(1);

		try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
			var approval = executor.submit(() -> {
				ready.countDown();
				go.await();
				try {
					service.approve(manager.getId(), leave.getId(), null);
					return "APPROVED";
				}
				catch (CoachLeaveException error) {
					return error.getCode();
				}
			});
			var expiry = executor.submit(() -> {
				ready.countDown();
				go.await();
				return lifecycle.catchUp(Instant.now());
			});
			ready.await();
			go.countDown();

			assertThat(approval.get()).isEqualTo("COACH_LEAVE_ALREADY_STARTED");
			assertThat(expiry.get()).isBetween(0, 1);
		}

		assertThat(db.queryForObject("select status from coach_leave_requests where id=?", String.class,
			leave.getId())).isEqualTo("EXPIRED");
		assertThat(db.queryForObject("select count(*) from audit_events where target_id=? and action=?", Long.class,
			leave.getId(), "COACH_LEAVE_EXPIRED")).isOne();
		assertThat(db.queryForObject("select count(*) from audit_events where target_id=? and action=?", Long.class,
			leave.getId(), "COACH_LEAVE_APPROVED")).isZero();
		verify(notifications, never()).write(any(), any(), eq("COACH_LEAVE_APPROVED"), any(), any(), any());

		assertThat(service.mine(coach.getId())).extracting(CoachLeaveResponse::status)
			.contains(CoachLeaveStatus.EXPIRED);
		assertThat(service.managerList(manager.getId(), CoachLeaveStatus.EXPIRED))
			.extracting(CoachLeaveResponse::id).contains(leave.getId());
	}

	private Account saveAccount(AccountRole role, String name) {
		UUID id = UUID.randomUUID();
		return accounts.saveAndFlush(new Account(id, role, AccountStatus.ACTIVE, name,
			"09" + id.toString().replace("-", "").substring(0, 8), id + "@example.test",
			LocalDate.of(1990, 1, 1), "{noop}password"));
	}
}
