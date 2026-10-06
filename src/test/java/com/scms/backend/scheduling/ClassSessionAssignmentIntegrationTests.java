package com.scms.backend.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
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
class ClassSessionAssignmentIntegrationTests {

	@Container
	@ServiceConnection
	static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

	@Autowired ClassSessionService service;
	@Autowired ClassSessionRepository sessions;
	@Autowired AccountRepository accounts;
	@Autowired DisciplineRepository disciplines;
	@Autowired SportClassRepository classes;
	@Autowired RoomRepository rooms;
	@Autowired JdbcTemplate jdbc;

	@Test
	@Transactional
	void updatesOnlyAssignmentAndRejectsConflictingResources() {
		Account manager = saveAccount(AccountRole.MANAGER, "Manager");
		Account coachA = saveAccount(AccountRole.COACH, "Coach A");
		Account coachB = saveAccount(AccountRole.COACH, "Coach B");
		Account coachC = saveAccount(AccountRole.COACH, "Coach C");
		Discipline discipline = disciplines.saveAndFlush(new Discipline(UUID.randomUUID(), "Yoga", null,
			DisciplineStatus.ACTIVE));
		SportClass sportClass = classes.saveAndFlush(new SportClass(UUID.randomUUID(), discipline, "Vinyasa",
			SportClassType.YOGA, null, SportClassStatus.ACTIVE));
		Room roomA = rooms.saveAndFlush(new Room(UUID.randomUUID(), "Studio A", 20, RoomStatus.ACTIVE));
		Room roomB = rooms.saveAndFlush(new Room(UUID.randomUUID(), "Studio B", 20, RoomStatus.ACTIVE));
		Room roomC = rooms.saveAndFlush(new Room(UUID.randomUUID(), "Studio C", 20, RoomStatus.ACTIVE));
		Instant start = Instant.parse("2030-01-01T02:00:00Z");
		Instant end = Instant.parse("2030-01-01T03:00:00Z");
		ClassSession target = sessions.saveAndFlush(new ClassSession(UUID.randomUUID(), sportClass, null, coachA,
			roomA, start, end, 12, manager.getId()));
		sessions.saveAndFlush(new ClassSession(UUID.randomUUID(), sportClass, null, coachC, roomC, start, end, 12,
			manager.getId()));

		service.updateAssignment(manager.getId(), target.getId(),
			new ClassSessionAssignmentRequest(coachB.getId(), roomB.getId()));

		Map<String, Object> row = jdbc.queryForMap("""
			select teaching_coach_account_id, room_id, start_time, end_time, capacity
			from class_sessions where id = ?
			""", target.getId());
		assertThat(row.get("teaching_coach_account_id")).isEqualTo(coachB.getId());
		assertThat(row.get("room_id")).isEqualTo(roomB.getId());
		assertThat(((java.sql.Timestamp) row.get("start_time")).toInstant()).isEqualTo(start);
		assertThat(((java.sql.Timestamp) row.get("end_time")).toInstant()).isEqualTo(end);
		assertThat(row.get("capacity")).isEqualTo(12);

		assertThatThrownBy(() -> service.updateAssignment(manager.getId(), target.getId(),
			new ClassSessionAssignmentRequest(coachC.getId(), roomC.getId())))
			.isInstanceOf(RecurringScheduleException.class)
			.extracting("code").isEqualTo("SESSION_CONFLICT");
	}

	private Account saveAccount(AccountRole role, String name) {
		UUID id = UUID.randomUUID();
		return accounts.saveAndFlush(new Account(id, role, AccountStatus.ACTIVE, name,
			"09" + id.toString().replace("-", "").substring(0, 8), id + "@example.test",
			LocalDate.of(1990, 1, 1), "{noop}password"));
	}
}
