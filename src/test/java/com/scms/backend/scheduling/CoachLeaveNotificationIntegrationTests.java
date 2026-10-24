package com.scms.backend.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.notification.NotificationOutboxDispatcher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@SpringBootTest(properties = "scms.notification-outbox-initial-delay-ms=3600000")
@ActiveProfiles("test")
class CoachLeaveNotificationIntegrationTests {

	@Container
	@ServiceConnection
	static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

	@Autowired CoachLeaveService leaves;
	@Autowired AccountRepository accounts;
	@Autowired NotificationOutboxDispatcher dispatcher;
	@Autowired JdbcTemplate db;

	@Test
	void routesEveryLeaveTransitionThroughIdempotentOutboxAndUnreadNotifications() {
		Account approvingManager = saveAccount(AccountRole.MANAGER, AccountStatus.ACTIVE, "Approving Manager");
		Account operationsManager = saveAccount(AccountRole.MANAGER, AccountStatus.ACTIVE, "Operations Manager");
		Account inactiveManager = saveAccount(AccountRole.MANAGER, AccountStatus.INACTIVE, "Inactive Manager");
		Account coach = saveAccount(AccountRole.COACH, AccountStatus.ACTIVE, "Notification Coach");
		Instant base = Instant.parse("2034-01-01T02:00:00Z");

		CoachLeaveResponse withdrawn = leaves.create(coach.getId(), request(base, "Withdrawn leave"));
		leaves.withdraw(coach.getId(), withdrawn.id());
		assertThatThrownBy(() -> leaves.withdraw(coach.getId(), withdrawn.id()))
			.isInstanceOfSatisfying(CoachLeaveException.class,
				error -> assertThat(error.getCode()).isEqualTo("COACH_LEAVE_NOT_PENDING"));

		CoachLeaveResponse cancelled = leaves.create(coach.getId(), request(base.plusSeconds(10800), "Cancelled leave"));
		leaves.approve(approvingManager.getId(), cancelled.id(), new CoachLeaveReviewRequest("Approved"));
		leaves.cancel(coach.getId(), cancelled.id());
		assertThatThrownBy(() -> leaves.cancel(coach.getId(), cancelled.id()))
			.isInstanceOfSatisfying(CoachLeaveException.class,
				error -> assertThat(error.getCode()).isEqualTo("COACH_LEAVE_NOT_APPROVED"));

		CoachLeaveResponse rejected = leaves.create(coach.getId(), request(base.plusSeconds(21600), "Rejected leave"));
		leaves.reject(approvingManager.getId(), rejected.id(), new CoachLeaveReviewRequest("Rejected"));

		for (UUID managerId : new UUID[]{approvingManager.getId(), operationsManager.getId()}) {
			assertOutboxEvent(withdrawn.id(), managerId, "COACH_LEAVE_REQUESTED", 1);
			assertOutboxEvent(withdrawn.id(), managerId, "COACH_LEAVE_WITHDRAWN", 1);
			assertOutboxEvent(cancelled.id(), managerId, "COACH_LEAVE_REQUESTED", 1);
			assertOutboxEvent(cancelled.id(), managerId, "COACH_LEAVE_CANCELLED", 1);
			assertOutboxEvent(rejected.id(), managerId, "COACH_LEAVE_REQUESTED", 1);
		}
		assertThat(outboxCount(inactiveManager.getId())).isZero();
		assertOutboxEvent(cancelled.id(), coach.getId(), "COACH_LEAVE_APPROVED", 1);
		assertOutboxEvent(rejected.id(), coach.getId(), "COACH_LEAVE_REJECTED", 1);
		assertThat(db.queryForObject("""
			select payload ->> 'coachFullName' from notification_outbox
			where target_id=? and recipient_account_id=? and notification_type='COACH_LEAVE_REQUESTED'
			""", String.class, withdrawn.id(), approvingManager.getId())).isEqualTo(coach.getFullName());

		dispatcher.dispatchAvailable();

		assertThat(unreadCount(approvingManager.getId())).isEqualTo(5);
		assertThat(unreadCount(operationsManager.getId())).isEqualTo(5);
		assertThat(unreadCount(coach.getId())).isEqualTo(2);
		assertThat(unreadCount(inactiveManager.getId())).isZero();
	}

	@Test
	void rolledBackLeaveRequestCreatesNoOutboxEvent() {
		Account manager = saveAccount(AccountRole.MANAGER, AccountStatus.ACTIVE, "Rollback Manager");
		Account coach = saveAccount(AccountRole.COACH, AccountStatus.ACTIVE, "Rollback Coach");
		long before = outboxCount(manager.getId());
		String constraint = "chk_audit_reject_coach_leave_requested";
		db.execute("alter table audit_events add constraint " + constraint
			+ " check (not (action='COACH_LEAVE_REQUESTED' and actor_account_id='" + coach.getId() + "'))");

		try {
			assertThatThrownBy(() -> leaves.create(coach.getId(), request(
				Instant.parse("2035-01-01T02:00:00Z"), "Rollback leave")))
				.rootCause().hasMessageContaining(constraint);
			assertThat(outboxCount(manager.getId())).isEqualTo(before);
		}
		finally {
			db.execute("alter table audit_events drop constraint " + constraint);
		}
	}

	private CoachLeaveCreateRequest request(Instant start, String reason) {
		return new CoachLeaveCreateRequest(start, start.plusSeconds(3600), reason);
	}

	private void assertOutboxEvent(UUID requestId, UUID recipientId, String type, int expected) {
		assertThat(db.queryForObject("""
			select count(*) from notification_outbox
			where target_id=? and recipient_account_id=? and notification_type=?
			""", Integer.class, requestId, recipientId, type)).isEqualTo(expected);
	}

	private long outboxCount(UUID recipientId) {
		return db.queryForObject("select count(*) from notification_outbox where recipient_account_id=?",
			Long.class, recipientId);
	}

	private long unreadCount(UUID recipientId) {
		return db.queryForObject("select count(*) from notifications where recipient_account_id=? and read_at is null",
			Long.class, recipientId);
	}

	private Account saveAccount(AccountRole role, AccountStatus status, String name) {
		UUID id = UUID.randomUUID();
		return accounts.saveAndFlush(new Account(id, role, status, name,
			"09" + id.toString().replace("-", "").substring(0, 8), id + "@example.test",
			LocalDate.of(1990, 1, 1), "{noop}password"));
	}
}
