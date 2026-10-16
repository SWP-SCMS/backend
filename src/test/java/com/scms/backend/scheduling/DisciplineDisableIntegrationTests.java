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
class DisciplineDisableIntegrationTests {

	private static final Instant PAST = Instant.parse("2025-01-01T00:00:00Z");
	private static final Instant FUTURE = Instant.parse("2030-01-01T00:00:00Z");

	@Container
	@ServiceConnection
	static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

	@Autowired DisciplineService disciplines;
	@Autowired ClassSessionService sessionsService;
	@Autowired AccountRepository accounts;
	@Autowired DisciplineRepository disciplineRepository;
	@Autowired SportClassRepository classes;
	@Autowired ClassSessionRepository sessions;
	@Autowired RoomRepository rooms;
	@Autowired JdbcTemplate db;
	@Autowired PlatformTransactionManager transactionManager;

	@Test
	@Transactional
	void staleScheduledAndInProgressSessionsBlockButCompletedHistoryDoesNot() {
		Fixture fixture = fixture();
		ClassSession staleScheduled = saveSession(fixture, PAST, FUTURE);
		ClassSession inProgress = saveSession(fixture, PAST.plusSeconds(3600), FUTURE);
		ClassSession completed = saveSession(fixture, PAST.plusSeconds(7200), FUTURE);
		db.update("update class_sessions set status='IN_PROGRESS' where id=?", inProgress.getId());
		db.update("update class_sessions set status='COMPLETED' where id=?", completed.getId());

		assertThatThrownBy(() -> disciplines.update(fixture.managerId(), fixture.disciplineId(),
			new DisciplinePatchRequest(null, null, DisciplineStatus.INACTIVE)))
			.isInstanceOf(DisciplineValidationException.class)
			.satisfies(error -> {
				DisciplineValidationException exception = (DisciplineValidationException) error;
				assertThat(exception.getAffectedSessions()).extracting(DisciplineSessionConflict::sessionId)
					.containsExactly(staleScheduled.getId(), inProgress.getId());
				assertThat(exception.getAffectedSessions()).extracting(DisciplineSessionConflict::classId)
					.containsExactly(fixture.classId(), fixture.classId());
			});
	}

	@Test
	@Transactional
	void completedSessionIsRetainedWhenDisciplineIsDisabled() {
		Fixture fixture = fixture();
		ClassSession completed = saveSession(fixture, PAST, FUTURE);
		db.update("update class_sessions set status='COMPLETED' where id=?", completed.getId());

		disciplines.update(fixture.managerId(), fixture.disciplineId(),
			new DisciplinePatchRequest(null, null, DisciplineStatus.INACTIVE));

		assertThat(disciplineRepository.findById(fixture.disciplineId()).orElseThrow().getStatus())
			.isEqualTo(DisciplineStatus.INACTIVE);
		assertThat(db.queryForObject("select count(*) from class_sessions where id=?", Long.class, completed.getId()))
			.isOne();
	}

	@Test
	void disableAndSessionCreateCannotBothPassTheClassLock() throws Exception {
		Fixture fixture = fixture();
		TransactionTemplate transactions = new TransactionTemplate(transactionManager);
		CountDownLatch ready = new CountDownLatch(2);
		CountDownLatch start = new CountDownLatch(1);
		try (var executor = Executors.newFixedThreadPool(2)) {
			var disable = executor.submit(() -> {
				ready.countDown();
				start.await(10, TimeUnit.SECONDS);
				try {
					transactions.executeWithoutResult(status -> disciplines.update(fixture.managerId(),
						fixture.disciplineId(), new DisciplinePatchRequest(null, null, DisciplineStatus.INACTIVE)));
					return "disabled";
				}
				catch (DisciplineValidationException exception) {
					return "blocked";
				}
			});
			var create = executor.submit(() -> {
				ready.countDown();
				start.await(10, TimeUnit.SECONDS);
				try {
					transactions.executeWithoutResult(status -> sessionsService.create(fixture.managerId(),
						new ClassSessionCreateRequest(fixture.classId(), fixture.coachId(), fixture.roomId(),
							FUTURE, FUTURE.plusSeconds(3600), 10)));
					return "created";
				}
				catch (RecurringScheduleException exception) {
					return "blocked";
				}
			});
			assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
			start.countDown();
			assertThat(java.util.List.of(disable.get(10, TimeUnit.SECONDS), create.get(10, TimeUnit.SECONDS)))
				.containsExactlyInAnyOrder("disabled", "created");
		}

		String status = db.queryForObject("select status from disciplines where id=?", String.class,
			fixture.disciplineId());
		long activeSessions = db.queryForObject("select count(*) from class_sessions where sport_class_id=? "
			+ "and status in ('SCHEDULED','IN_PROGRESS')", Long.class, fixture.classId());
		assertThat(status).isIn("ACTIVE", "INACTIVE");
		if ("INACTIVE".equals(status)) assertThat(activeSessions).isZero();
		else assertThat(activeSessions).isOne();
	}

	private ClassSession saveSession(Fixture fixture, Instant start, Instant end) {
		Account coach = saveAccount(AccountRole.COACH, "Session coach");
		Room room = rooms.saveAndFlush(new Room(UUID.randomUUID(), "Session studio " + UUID.randomUUID(), 20,
			RoomStatus.ACTIVE));
		return sessions.saveAndFlush(new ClassSession(UUID.randomUUID(), fixture.sportClass(), null,
			coach, room, start, end, 10, fixture.managerId()));
	}

	private Fixture fixture() {
		Account manager = saveAccount(AccountRole.MANAGER, "Manager");
		Account coach = saveAccount(AccountRole.COACH, "Coach");
		Discipline discipline = disciplineRepository.saveAndFlush(new Discipline(UUID.randomUUID(),
			"Yoga " + UUID.randomUUID(), null, DisciplineStatus.ACTIVE));
		SportClass sportClass = classes.saveAndFlush(new SportClass(UUID.randomUUID(), discipline, "Vinyasa",
			SportClassType.GROUP, null, SportClassStatus.ACTIVE));
		Room room = rooms.saveAndFlush(new Room(UUID.randomUUID(), "Studio " + UUID.randomUUID(), 20,
			RoomStatus.ACTIVE));
		return new Fixture(manager.getId(), discipline.getId(), sportClass.getId(), coach.getId(), room.getId(),
			sportClass, coach, room);
	}

	private Account saveAccount(AccountRole role, String name) {
		UUID id = UUID.randomUUID();
		return accounts.saveAndFlush(new Account(id, role, AccountStatus.ACTIVE, name,
			"09" + id.toString().replace("-", "").substring(0, 8), id + "@example.test",
			LocalDate.of(1990, 1, 1), "{noop}password"));
	}

	private record Fixture(UUID managerId, UUID disciplineId, UUID classId, UUID coachId, UUID roomId,
		SportClass sportClass, Account coach, Room room) { }
}
