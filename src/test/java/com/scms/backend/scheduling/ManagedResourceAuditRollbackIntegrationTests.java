package com.scms.backend.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import jakarta.persistence.EntityManager;
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
	@Autowired EntityManager entityManager;

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

	@Test
	void auditFailureRollsBackDisciplineUpdate() {
		Account manager = saveManager("Discipline update manager");
		Discipline discipline = disciplines.saveAndFlush(new Discipline(UUID.randomUUID(), "Original discipline",
			"Original description", DisciplineStatus.ACTIVE));
		long originalVersion = version("disciplines", discipline.getId());
		String constraint = "chk_test_reject_discipline_update_audit";
		installFailureConstraint(constraint, "DISCIPLINE_UPDATED");

		try {
			assertThatThrownBy(() -> disciplineService.update(manager.getId(), discipline.getId(),
				new DisciplinePatchRequest("Changed discipline", "Changed description", DisciplineStatus.INACTIVE)))
				.rootCause().hasMessageContaining(constraint);
			entityManager.clear();

			Map<String, Object> row = jdbc.queryForMap(
				"select name, description, status::text as status, version from disciplines where id = ?",
				discipline.getId());
			assertThat(row).containsEntry("name", "Original discipline")
				.containsEntry("description", "Original description")
				.containsEntry("status", "ACTIVE");
			assertThat(((Number) row.get("version")).longValue()).isEqualTo(originalVersion);
			assertNoAudit("DISCIPLINE_UPDATED", discipline.getId());
		}
		finally {
			dropConstraint(constraint);
		}
	}

	@Test
	void auditFailureRollsBackSportClassUpdate() {
		Account manager = saveManager("Class update manager");
		Discipline discipline = disciplines.saveAndFlush(new Discipline(UUID.randomUUID(), "Update class parent",
			null, DisciplineStatus.ACTIVE));
		SportClass sportClass = classes.saveAndFlush(new SportClass(UUID.randomUUID(), discipline, "Original class",
			SportClassType.GROUP, "Original description", SportClassStatus.ACTIVE));
		long originalVersion = version("sport_classes", sportClass.getId());
		String constraint = "chk_test_reject_sport_class_update_audit";
		installFailureConstraint(constraint, "SPORT_CLASS_UPDATED");

		try {
			assertThatThrownBy(() -> sportClassService.update(manager.getId(), sportClass.getId(),
				new SportClassPatchRequest("Changed class", SportClassType.PT_1_1,
					"Changed description", SportClassStatus.INACTIVE)))
				.rootCause().hasMessageContaining(constraint);
			entityManager.clear();

			Map<String, Object> row = jdbc.queryForMap("""
				select discipline_id, name, class_type::text as class_type, description,
					status::text as status, version
				from sport_classes where id = ?
				""", sportClass.getId());
			assertThat(row).containsEntry("discipline_id", discipline.getId())
				.containsEntry("name", "Original class")
				.containsEntry("class_type", "GROUP")
				.containsEntry("description", "Original description")
				.containsEntry("status", "ACTIVE");
			assertThat(((Number) row.get("version")).longValue()).isEqualTo(originalVersion);
			assertNoAudit("SPORT_CLASS_UPDATED", sportClass.getId());
		}
		finally {
			dropConstraint(constraint);
		}
	}

	@Test
	void auditFailureRollsBackRoomUpdate() {
		Account manager = saveManager("Room update manager");
		Room room = rooms.saveAndFlush(new Room(UUID.randomUUID(), "Original room", 20, RoomStatus.ACTIVE));
		long originalVersion = version("rooms", room.getId());
		String constraint = "chk_test_reject_room_update_audit";
		installFailureConstraint(constraint, "ROOM_UPDATED");

		try {
			assertThatThrownBy(() -> roomService.update(manager.getId(), room.getId(),
				new RoomPatchRequest("Changed room", 30, RoomStatus.INACTIVE)))
				.rootCause().hasMessageContaining(constraint);
			entityManager.clear();

			Map<String, Object> row = jdbc.queryForMap(
				"select name, capacity, status::text as status, version from rooms where id = ?", room.getId());
			assertThat(row).containsEntry("name", "Original room")
				.containsEntry("capacity", 20)
				.containsEntry("status", "ACTIVE");
			assertThat(((Number) row.get("version")).longValue()).isEqualTo(originalVersion);
			assertNoAudit("ROOM_UPDATED", room.getId());
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

	private long version(String table, UUID id) {
		return jdbc.queryForObject("select version from " + table + " where id = ?", Long.class, id);
	}

	private void assertNoAudit(String action, UUID targetId) {
		assertThat(jdbc.queryForObject(
			"select count(*) from audit_events where action = ? and target_id = ?", Long.class, action, targetId))
			.isZero();
	}
}
