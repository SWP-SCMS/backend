package com.scms.backend.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
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
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
class ManagedResourceAuditRollbackIntegrationTests {

	@Container
	@ServiceConnection
	static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

	@Autowired DisciplineService disciplineService;
	@Autowired SportClassService sportClassService;
	@Autowired RoomService roomService;
	@Autowired DisciplineRepository disciplines;
	@Autowired SportClassRepository classes;
	@Autowired RoomRepository rooms;
	@Autowired AccountRepository accounts;
	@Autowired JdbcTemplate jdbc;

	@Test
	void auditFailureRollsBackDisciplineCreation() {
		Account manager = saveManager("Discipline manager");
		long before = disciplines.count();
		String constraint = "chk_test_reject_discipline_audit";
		installFailureConstraint(constraint, "DISCIPLINE_CREATED");

		try {
			assertThatThrownBy(() -> disciplineService.create(manager.getId(),
				new DisciplineCreateRequest("Rollback discipline", "Must roll back")))
				.rootCause().hasMessageContaining(constraint);
			assertThat(disciplines.count()).isEqualTo(before);
		}
		finally {
			dropConstraint(constraint);
		}
	}

	@Test
	void auditFailureRollsBackSportClassCreation() {
		Account manager = saveManager("Class manager");
		Discipline discipline = disciplines.saveAndFlush(new Discipline(UUID.randomUUID(), "Class parent",
			null, DisciplineStatus.ACTIVE));
		long before = classes.count();
		String constraint = "chk_test_reject_sport_class_audit";
		installFailureConstraint(constraint, "SPORT_CLASS_CREATED");

		try {
			assertThatThrownBy(() -> sportClassService.create(manager.getId(), new SportClassCreateRequest(
				discipline.getId(), "Rollback class", SportClassType.GROUP, "Must roll back")))
				.rootCause().hasMessageContaining(constraint);
			assertThat(classes.count()).isEqualTo(before);
		}
		finally {
			dropConstraint(constraint);
		}
	}

	@Test
	void auditFailureRollsBackRoomCreation() {
		Account manager = saveManager("Room manager");
		long before = rooms.count();
		String constraint = "chk_test_reject_room_audit";
		installFailureConstraint(constraint, "ROOM_CREATED");

		try {
			assertThatThrownBy(() -> roomService.create(manager.getId(),
				new RoomCreateRequest("Rollback room", 20)))
				.rootCause().hasMessageContaining(constraint);
			assertThat(rooms.count()).isEqualTo(before);
		}
		finally {
			dropConstraint(constraint);
		}
	}

	private Account saveManager(String name) {
		UUID id = UUID.randomUUID();
		return accounts.saveAndFlush(new Account(id, AccountRole.MANAGER, AccountStatus.ACTIVE, name,
			"09" + id.toString().replace("-", "").substring(0, 8), id + "@example.test",
			LocalDate.of(1990, 1, 1), "{noop}password"));
	}

	private void installFailureConstraint(String constraint, String action) {
		jdbc.execute("alter table audit_events add constraint " + constraint + " check (action <> '" + action + "')");
	}

	private void dropConstraint(String constraint) {
		jdbc.execute("alter table audit_events drop constraint " + constraint);
	}
}
