package com.scms.backend.scheduling;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
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
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
class RoomImpactIntegrationTests {

	@Container
	@ServiceConnection
	static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

	@Autowired RoomSuspensionService service;
	@Autowired AccountRepository accounts;
	@Autowired DisciplineRepository disciplines;
	@Autowired SportClassRepository classes;
	@Autowired RoomRepository rooms;
	@Autowired ClassSessionRepository sessions;
	@Autowired JdbcTemplate jdbc;

	@Test
	void permanentDisableMovesFutureSessionAndKeepsCompletedHistory() {
		Account manager = saveAccount(AccountRole.MANAGER, "Impact Manager");
		Account coach = saveAccount(AccountRole.COACH, "Impact Coach");
		Discipline discipline = disciplines.saveAndFlush(new Discipline(UUID.randomUUID(), "Impact Discipline", null,
			DisciplineStatus.ACTIVE));
		SportClass sportClass = classes.saveAndFlush(new SportClass(UUID.randomUUID(), discipline, "Impact Class",
			SportClassType.GROUP, null, SportClassStatus.ACTIVE));
		Room source = rooms.saveAndFlush(new Room(UUID.randomUUID(), "Impact Source", 20, RoomStatus.ACTIVE));
		Room target = rooms.saveAndFlush(new Room(UUID.randomUUID(), "Impact Target", 20, RoomStatus.ACTIVE));
		ClassSession future = sessions.saveAndFlush(new ClassSession(UUID.randomUUID(), sportClass, null, coach, source,
			Instant.parse("2030-01-01T02:00:00Z"), Instant.parse("2030-01-01T03:00:00Z"), 10, manager.getId()));
		ClassSession completed = sessions.saveAndFlush(new ClassSession(UUID.randomUUID(), sportClass, null, coach, source,
			Instant.parse("2020-01-01T02:00:00Z"), Instant.parse("2020-01-01T03:00:00Z"), 10, manager.getId()));
		jdbc.update("update class_sessions set status='COMPLETED' where id=?", completed.getId());

		RoomImpactPreviewResponse preview = service.previewImpact(manager.getId(), source.getId(),
			new RoomImpactPreviewRequest(null, null, true));

		assertThat(preview.count()).isOne();
		assertThat(preview.sessions()).singleElement().extracting(RoomImpactPreviewResponse.SessionImpact::sessionId)
			.isEqualTo(future.getId());

		RoomResponse result = service.applyImpact(manager.getId(), source.getId(), new RoomImpactPreviewRequest(
			null, null, true, "Room retired", List.of(new RoomSessionResolutionRequest(
			future.getId(), "MOVE", target.getId(), null, null, "Move to replacement room"))));

		assertThat(result.status()).isEqualTo(RoomStatus.INACTIVE);
		assertThat(jdbc.queryForObject("select room_id from class_sessions where id=?", UUID.class, future.getId()))
			.isEqualTo(target.getId());
		assertThat(sessions.findById(completed.getId()).orElseThrow().getStatus())
			.isEqualTo(ClassSessionStatus.COMPLETED);
		assertThat(jdbc.queryForObject("select room_id from class_sessions where id=?", UUID.class, completed.getId()))
			.isEqualTo(source.getId());
	}

	private Account saveAccount(AccountRole role, String name) {
		UUID id = UUID.randomUUID();
		return accounts.saveAndFlush(new Account(id, role, AccountStatus.ACTIVE, name,
			"09" + id.toString().replace("-", "").substring(0, 8), id + "@example.test",
			LocalDate.of(1990, 1, 1), "{noop}password"));
	}
}
