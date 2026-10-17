package com.scms.backend.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
class SportClassDeactivateIntegrationTests {

	@Container
	@ServiceConnection
	static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

	@Autowired SportClassService sportClasses;
	@Autowired ClassSessionService sessionService;
	@Autowired AccountRepository accounts;
	@Autowired DisciplineRepository disciplines;
	@Autowired SportClassRepository classes;
	@Autowired ClassSessionRepository sessions;
	@Autowired RoomRepository rooms;
	@Autowired JdbcTemplate db;
	@Autowired PlatformTransactionManager transactionManager;

	@Test
	@Transactional
	void scheduledAndInProgressSessionsThatHaveNotEndedBlockDeactivation() {
		Fixture fixture = fixture();
		Instant now = Instant.now();
		ClassSession scheduled = saveSession(fixture, now.plusSeconds(600), now.plusSeconds(4200));
		ClassSession inProgress = saveSession(fixture, now.minusSeconds(600), now.plusSeconds(3000));
		db.update("update class_sessions set status='IN_PROGRESS' where id=?", inProgress.getId());

		assertThatThrownBy(() -> sportClasses.update(fixture.managerId(), fixture.sportClass().getId(),
			new SportClassPatchRequest(null, null, null, SportClassStatus.INACTIVE)))
			.isInstanceOf(SportClassException.class)
			.satisfies(error -> {
				SportClassException exception = (SportClassException) error;
				assertThat(exception.getCode())
					.isEqualTo("CLASS_HAS_ACTIVE_SESSIONS");
				assertThat(exception.getAffectedSessions())
					.extracting(DisciplineSessionConflict::sessionId)
					.containsExactly(inProgress.getId(), scheduled.getId());
			});
	}

	@Test
	@Transactional
	void endedAndCompletedSessionsRemainAsHistoryAndDoNotBlockDeactivation() {
		Fixture fixture = fixture();
		Instant past = Instant.now().minusSeconds(7200);
		ClassSession ended = saveSession(fixture, past, past.plusSeconds(1800));
		ClassSession completed = saveSession(fixture, past, past.plusSeconds(3600));
		db.update("update class_sessions set status='COMPLETED' where id=?", completed.getId());

		sportClasses.update(fixture.managerId(), fixture.sportClass().getId(),
			new SportClassPatchRequest(null, null, null, SportClassStatus.INACTIVE));

		assertThat(classes.findById(fixture.sportClass().getId()).orElseThrow()
			.getStatus()).isEqualTo(SportClassStatus.INACTIVE);
		assertThat(db.queryForObject(
			"select count(*) from class_sessions where id in (?,?)", Long.class, ended.getId(), completed.getId()))
			.isEqualTo(2L);
	}

	@Test
	void deactivateAndSessionCreateCannotBothPassTheClassLock() throws Exception {
		Fixture fixture = fixture();
		TransactionTemplate transactions = new TransactionTemplate(transactionManager);
		CountDownLatch ready = new CountDownLatch(2);
		CountDownLatch start = new CountDownLatch(1);
		try (var executor = Executors.newFixedThreadPool(2)) {
			var deactivate = executor.submit(() -> {
				ready.countDown();
				start.await(10, TimeUnit.SECONDS);
				try {
					transactions.executeWithoutResult(status -> sportClasses.update(fixture.managerId(),
						fixture.sportClass().getId(),
						new SportClassPatchRequest(null, null, null, SportClassStatus.INACTIVE)));
					return "deactivated";
				}
				catch (SportClassException exception) {
					return "blocked";
				}
			});
			var create = executor.submit(() -> {
				ready.countDown();
				start.await(10, TimeUnit.SECONDS);
				try {
					Instant future = Instant.now().plusSeconds(3600);
					transactions.executeWithoutResult(status -> sessionService.create(fixture.managerId(),
						new ClassSessionCreateRequest(fixture.sportClass().getId(), fixture.coach().getId(),
							fixture.room().getId(), future, future.plusSeconds(3600), 10)));
					return "created";
				}
				catch (RecurringScheduleException exception) {
					return "blocked";
				}
			});
			assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
			start.countDown();
			assertThat(java.util.List.of(
				deactivate.get(10, TimeUnit.SECONDS), create.get(10, TimeUnit.SECONDS)))
				.isIn(java.util.List.of("deactivated", "blocked"), java.util.List.of("blocked", "created"));
		}

		String status = db.queryForObject("select status from sport_classes where id=?", String.class,
			fixture.sportClass().getId());
		long blockers = db.queryForObject("select count(*) from class_sessions where sport_class_id=? "
			+ "and status in ('SCHEDULED','IN_PROGRESS') and end_time > current_timestamp", Long.class,
			fixture.sportClass().getId());
		if ("INACTIVE".equals(status)) assertThat(blockers).isZero();
		else assertThat(blockers).isOne();
	}

	private ClassSession saveSession(Fixture fixture, Instant start, Instant end) {
		Account coach = saveAccount(AccountRole.COACH, "Session coach");
		Room room = rooms.saveAndFlush(new Room(UUID.randomUUID(), "Session room " + UUID.randomUUID(), 20,
			RoomStatus.ACTIVE));
		return sessions.saveAndFlush(new ClassSession(UUID.randomUUID(), fixture.sportClass(), null,
			coach, room, start, end, 10, fixture.managerId()));
	}

	private Fixture fixture() {
		Account manager = saveAccount(AccountRole.MANAGER, "Manager");
		Account coach = saveAccount(AccountRole.COACH, "Coach");
		Discipline discipline = disciplines.saveAndFlush(new Discipline(UUID.randomUUID(),
			"Discipline " + UUID.randomUUID(), null, DisciplineStatus.ACTIVE));
		SportClass sportClass = classes.saveAndFlush(new SportClass(UUID.randomUUID(), discipline, "Class",
			SportClassType.GROUP, null, SportClassStatus.ACTIVE));
		Room room = rooms.saveAndFlush(new Room(UUID.randomUUID(), "Room " + UUID.randomUUID(), 20,
			RoomStatus.ACTIVE));
		return new Fixture(manager.getId(), sportClass, coach, room);
	}

	private Account saveAccount(AccountRole role, String name) {
		UUID id = UUID.randomUUID();
		return accounts.saveAndFlush(new Account(id, role, AccountStatus.ACTIVE, name,
			"09" + id.toString().replace("-", "").substring(0, 8), id + "@example.test",
			LocalDate.of(1990, 1, 1), "{noop}password"));
	}

	private record Fixture(UUID managerId, SportClass sportClass, Account coach, Room room) { }
}
