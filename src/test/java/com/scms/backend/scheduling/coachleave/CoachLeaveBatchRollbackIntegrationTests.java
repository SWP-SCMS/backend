package com.scms.backend.scheduling.coachleave;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.scheduling.ClassSession;
import com.scms.backend.scheduling.CoachLeaveException;
import com.scms.backend.scheduling.CoachLeaveRequest;
import com.scms.backend.scheduling.Discipline;
import com.scms.backend.scheduling.DisciplineStatus;
import com.scms.backend.scheduling.Room;
import com.scms.backend.scheduling.RoomStatus;
import com.scms.backend.scheduling.SportClass;
import com.scms.backend.scheduling.SportClassStatus;
import com.scms.backend.scheduling.SportClassType;
import com.scms.backend.scheduling.discipline.DisciplineRepository;
import com.scms.backend.scheduling.room.RoomRepository;
import com.scms.backend.scheduling.session.ClassSessionRepository;
import com.scms.backend.scheduling.sportclass.SportClassRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
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
class CoachLeaveBatchRollbackIntegrationTests {

	@Container
	@ServiceConnection
	static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

	@Autowired
	CoachLeaveBatchService batches;

	@Autowired
	CoachLeaveRepository leaves;

	@Autowired
	ClassSessionRepository sessions;

	@Autowired
	AccountRepository accounts;

	@Autowired
	DisciplineRepository disciplines;

	@Autowired
	SportClassRepository classes;

	@Autowired
	RoomRepository rooms;

	@Autowired
	JdbcTemplate db;

	@Test
	void secondSessionConflictRollsBackFirstAssignmentAndAuditInPostgres() {
		Account manager = account(AccountRole.MANAGER, "Batch Manager");
		Account original = account(AccountRole.COACH, "Original Coach");
		Account replacement = account(AccountRole.COACH, "Replacement Coach");
		Instant start = Instant.parse("2034-02-01T02:00:00Z");
		Discipline discipline = disciplines.saveAndFlush(new Discipline(UUID.randomUUID(), "Batch Pilates", null, DisciplineStatus.ACTIVE));
		SportClass sportClass = classes.saveAndFlush(new SportClass(UUID.randomUUID(), discipline, "Batch Class", SportClassType.GROUP, null, SportClassStatus.ACTIVE));
		Room room = rooms.saveAndFlush(new Room(UUID.randomUUID(), "Batch Studio", 20, RoomStatus.ACTIVE));
		Room otherRoom = rooms.saveAndFlush(new Room(UUID.randomUUID(), "Busy Studio", 20, RoomStatus.ACTIVE));
		long prefix = UUID.randomUUID().getMostSignificantBits();
		ClassSession first = sessions.saveAndFlush(new ClassSession(new UUID(prefix, 1), sportClass, null, original, room, start, start.plusSeconds(3600), 12, manager.getId()));
		ClassSession second = sessions.saveAndFlush(new ClassSession(new UUID(prefix, 2), sportClass, null, original, room, start.plusSeconds(86400), start.plusSeconds(90000), 12, manager.getId()));
		sessions.saveAndFlush(new ClassSession(UUID.randomUUID(), sportClass, null, replacement, otherRoom, second.getStartTime(), second.getEndTime(), 12, manager.getId()));
		CoachLeaveRequest leave = leaves.saveAndFlush(new CoachLeaveRequest(UUID.randomUUID(), original, start.minusSeconds(60), second.getEndTime().plusSeconds(60), "Batch leave"));
		var firstSelection = new CoachLeaveBatchService.Selection("CLASS", first.getId(), first.getVersion());
		var secondSelection = new CoachLeaveBatchService.Selection("CLASS", second.getId(), second.getVersion());
		var command = new CoachLeaveBatchService.Command(List.of(firstSelection, secondSelection), "reassign", replacement.getId(), "Cover leave");
		assertThatThrownBy(() -> batches.resolve(manager.getId(), leave.getId(), command)).isInstanceOf(CoachLeaveException.class).hasFieldOrPropertyWithValue("code", "COACH_LEAVE_BATCH_FAILED").hasFieldOrPropertyWithValue("affectedSessionIds", List.of(second.getId()));
		for (ClassSession session : List.of(first, second)) {
			assertThat(db.queryForObject("select teaching_coach_account_id from class_sessions where id=?", UUID.class, session.getId())).isEqualTo(original.getId());
			assertThat(db.queryForObject("select version from class_sessions where id=?", Long.class, session.getId())).isEqualTo(session.getVersion());
			assertThat(db.queryForObject("select count(*) from audit_events where target_id=?", Long.class, session.getId())).isZero();
		}
		assertThat(db.queryForObject("select status from coach_leave_requests where id=?", String.class, leave.getId())).isEqualTo("PENDING");
		assertThat(batches.resolve(manager.getId(), leave.getId(), new CoachLeaveBatchService.Command(List.of(firstSelection), "reassign", replacement.getId(), "Cover leave")).processedCount()).isOne();
		assertThat(db.queryForObject("select teaching_coach_account_id from class_sessions where id=?", UUID.class, first.getId())).isEqualTo(replacement.getId());
		assertThat(db.queryForObject("select count(*) from audit_events where target_id=?", Long.class, first.getId())).isPositive();
	}

	private Account account(AccountRole role, String name) {
		UUID id = UUID.randomUUID();
		return accounts.saveAndFlush(new Account(id, role, AccountStatus.ACTIVE, name, "09" + id.toString().replace("-", "").substring(0, 8), id + "@example.test", LocalDate.of(1990, 1, 1), "{noop}password"));
	}
}
