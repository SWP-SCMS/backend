package com.scms.backend.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

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
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
class RecurringScheduleIntegrationTests {

	@Container
	@ServiceConnection
	static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

	@Autowired RecurringScheduleService service;
	@Autowired AccountRepository accounts;
	@Autowired DisciplineRepository disciplines;
	@Autowired SportClassRepository classes;
	@Autowired RoomRepository rooms;
	@Autowired JdbcTemplate jdbc;

	@Test
	@Transactional
	void persistsOneScheduleAndExactlyThirtySessionsAtomically() {
		Fixture fixture = fixture();

		RecurringScheduleResponse response = service.create(fixture.managerId(), request(fixture));

		assertThat(response.sessionIds()).hasSize(30).doesNotHaveDuplicates();
		assertThat(jdbc.queryForObject("select count(*) from recurring_schedules where id = ?", Long.class,
			response.id())).isEqualTo(1L);
		assertThat(jdbc.queryForObject("select count(*) from class_sessions where recurring_schedule_id = ?",
			Long.class, response.id())).isEqualTo(30L);
		assertThat(jdbc.queryForList("select distinct status from class_sessions where recurring_schedule_id = ?",
			String.class, response.id())).containsExactly("SCHEDULED");
	}

	@Test
	@Transactional
	void oneOverlappingOccurrenceRejectsTheWholeSecondBatch() {
		Fixture fixture = fixture();
		service.create(fixture.managerId(), request(fixture));

		assertThatThrownBy(() -> service.create(fixture.managerId(), request(fixture)))
			.isInstanceOf(RecurringScheduleException.class)
			.extracting("code").isEqualTo("SESSION_CONFLICT");
		assertThat(jdbc.queryForObject("select count(*) from recurring_schedules", Long.class)).isEqualTo(1L);
		assertThat(jdbc.queryForObject("select count(*) from class_sessions", Long.class)).isEqualTo(30L);
	}

	private RecurringScheduleCreateRequest request(Fixture fixture) {
		return new RecurringScheduleCreateRequest(fixture.classId(), fixture.coachId(), fixture.roomId(),
			LocalDate.of(2030, 1, 1), List.of(1, 3, 5), LocalTime.of(9, 0), LocalTime.of(10, 0), 12);
	}

	private Fixture fixture() {
		Account manager = saveAccount(AccountRole.MANAGER, "Manager");
		Account coach = saveAccount(AccountRole.COACH, "Coach");
		Discipline discipline = disciplines.saveAndFlush(new Discipline(UUID.randomUUID(), "Yoga", null,
			DisciplineStatus.ACTIVE));
		SportClass sportClass = classes.saveAndFlush(new SportClass(UUID.randomUUID(), discipline, "Vinyasa",
			SportClassType.YOGA, null, SportClassStatus.ACTIVE));
		Room room = rooms.saveAndFlush(new Room(UUID.randomUUID(), "Studio A", 20, RoomStatus.ACTIVE));
		return new Fixture(manager.getId(), coach.getId(), sportClass.getId(), room.getId());
	}

	private Account saveAccount(AccountRole role, String name) {
		UUID id = UUID.randomUUID();
		return accounts.saveAndFlush(new Account(id, role, AccountStatus.ACTIVE, name,
			"09" + id.toString().replace("-", "").substring(0, 8), id + "@example.test",
			LocalDate.of(1990, 1, 1), "{noop}password"));
	}

	private record Fixture(UUID managerId, UUID coachId, UUID classId, UUID roomId) { }
}
