package com.scms.backend.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
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
class CoachLeaveCancellationIntegrationTests {

	@Container
	@ServiceConnection
	static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

	@Autowired CoachLeaveService leaveService;
	@Autowired ClassSessionService sessionService;
	@Autowired CoachLeaveRepository leaves;
	@Autowired ClassSessionRepository sessions;
	@Autowired AccountRepository accounts;
	@Autowired DisciplineRepository disciplines;
	@Autowired SportClassRepository classes;
	@Autowired RoomRepository rooms;
	@Autowired JdbcTemplate db;
	@MockitoBean NotificationWriter notifications;

	@Test
	void cancellationRestoresAvailabilityWithoutChangingAdjustedSessionsAndNotifiesApprover() {
		Account manager = saveAccount(AccountRole.MANAGER, "Approving Manager");
		Account coach = saveAccount(AccountRole.COACH, "Returning Coach");
		Account assignedCoach = saveAccount(AccountRole.COACH, "Assigned Coach");
		Instant start = Instant.parse("2032-02-01T02:00:00Z"); Instant end = start.plusSeconds(3600);
		Discipline discipline = disciplines.saveAndFlush(new Discipline(UUID.randomUUID(), "Cancel Pilates", null,
			DisciplineStatus.ACTIVE));
		SportClass sportClass = classes.saveAndFlush(new SportClass(UUID.randomUUID(), discipline,
			"Cancel Class", SportClassType.GROUP, null, SportClassStatus.ACTIVE));
		Room room = rooms.saveAndFlush(new Room(UUID.randomUUID(), "Cancel Studio", 20, RoomStatus.ACTIVE));
		ClassSession adjustedSession = sessions.saveAndFlush(new ClassSession(UUID.randomUUID(), sportClass, null,
			assignedCoach, room, start, end, 12, manager.getId()));
		CoachLeaveRequest leave = approvedLeave(manager, coach, start.minusSeconds(60), end.plusSeconds(60));

		assertThat(sessionService.availableCoaches(manager.getId(), adjustedSession.getId()))
			.extracting(AvailableCoachResponse::coachAccountId).doesNotContain(coach.getId());

		CoachLeaveResponse response = leaveService.cancel(coach.getId(), leave.getId());

		assertThat(response.status()).isEqualTo(CoachLeaveStatus.CANCELLED);
		assertThat(response.reviewedBy()).isEqualTo(manager.getId());
		assertThat(sessionService.availableCoaches(manager.getId(), adjustedSession.getId()))
			.extracting(AvailableCoachResponse::coachAccountId).contains(coach.getId());
		assertThat(db.queryForObject("select teaching_coach_account_id from class_sessions where id=?", UUID.class,
			adjustedSession.getId())).isEqualTo(assignedCoach.getId());
		assertThat(db.queryForObject("select count(*) from audit_events where target_id=? and action=?", Long.class,
			leave.getId(), "COACH_LEAVE_CANCELLED")).isOne();
		verify(notifications).write(any(), eq(manager.getId()), eq("COACH_LEAVE_CANCELLED"),
			eq("COACH_LEAVE_REQUEST"), eq(leave.getId()), any());
		assertThat(leaveService.mine(coach.getId())).extracting(CoachLeaveResponse::status)
			.contains(CoachLeaveStatus.CANCELLED);
	}

	@Test
	void concurrentCancellationCreatesOneTransitionAuditAndNotification() throws Exception {
		Account manager = saveAccount(AccountRole.MANAGER, "Concurrent Manager");
		Account coach = saveAccount(AccountRole.COACH, "Concurrent Coach");
		CoachLeaveRequest leave = approvedLeave(manager, coach, Instant.parse("2033-01-01T02:00:00Z"),
			Instant.parse("2033-01-01T03:00:00Z"));
		CountDownLatch ready = new CountDownLatch(2); CountDownLatch go = new CountDownLatch(1);

		try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
			var first = executor.submit(() -> cancelResult(coach.getId(), leave.getId(), ready, go));
			var second = executor.submit(() -> cancelResult(coach.getId(), leave.getId(), ready, go));
			ready.await(); go.countDown();
			assertThat(List.of(first.get(), second.get()))
				.containsExactlyInAnyOrder("CANCELLED", "COACH_LEAVE_NOT_APPROVED");
		}

		assertThat(db.queryForObject("select status from coach_leave_requests where id=?", String.class,
			leave.getId())).isEqualTo("CANCELLED");
		assertThat(db.queryForObject("select count(*) from audit_events where target_id=? and action=?", Long.class,
			leave.getId(), "COACH_LEAVE_CANCELLED")).isOne();
		verify(notifications, times(1)).write(any(), eq(manager.getId()), eq("COACH_LEAVE_CANCELLED"),
			eq("COACH_LEAVE_REQUEST"), eq(leave.getId()), any());
	}

	private String cancelResult(UUID coachId, UUID leaveId, CountDownLatch ready, CountDownLatch go)
			throws InterruptedException {
		ready.countDown(); go.await();
		try { return leaveService.cancel(coachId, leaveId).status().name(); }
		catch (CoachLeaveException error) { return error.getCode(); }
	}

	private CoachLeaveRequest approvedLeave(Account manager, Account coach, Instant start, Instant end) {
		CoachLeaveRequest leave = new CoachLeaveRequest(UUID.randomUUID(), coach, start, end, "Approved leave");
		leave.approve(manager.getId(), "Approved", Instant.parse("2026-01-01T00:00:00Z"));
		return leaves.saveAndFlush(leave);
	}

	private Account saveAccount(AccountRole role, String name) {
		UUID id = UUID.randomUUID();
		return accounts.saveAndFlush(new Account(id, role, AccountStatus.ACTIVE, name,
			"09" + id.toString().replace("-", "").substring(0, 8), id + "@example.test",
			LocalDate.of(1990, 1, 1), "{noop}password"));
	}
}
