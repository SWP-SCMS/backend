package com.scms.backend.scheduling;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
@Import(ClassSessionListIntegrationTests.FixedClockConfiguration.class)
class ClassSessionListIntegrationTests {

	private static final Instant NOW = Instant.parse("2030-01-01T03:00:00Z");

	@Container
	@ServiceConnection
	static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

	@Autowired ClassSessionService service;
	@Autowired ClassSessionRepository sessions;
	@Autowired AccountRepository accounts;
	@Autowired DisciplineRepository disciplines;
	@Autowired SportClassRepository classes;
	@Autowired RoomRepository rooms;

	@Test
	@Transactional
	void filtersCalendarAndAdvancesLifecycleWithoutRemovingHistory() {
		Fixture fixture = fixture();
		sessions.saveAndFlush(session(fixture, NOW.minusSeconds(7200), NOW.minusSeconds(3600)));
		sessions.saveAndFlush(session(fixture, NOW.minusSeconds(1800), NOW.plusSeconds(1800)));
		sessions.saveAndFlush(session(fixture, NOW.plusSeconds(3600), NOW.plusSeconds(7200)));
		PageRequest page = PageRequest.of(0, 20,
			Sort.by("startTime").ascending().and(Sort.by("id")));

		ClassSessionPageResponse all = service.list(fixture.managerId(), NOW.minusSeconds(10800),
			NOW.plusSeconds(10800), fixture.classId(), fixture.coachId(), fixture.roomId(), null, page);

		assertThat(all.content()).extracting(ClassSessionResponse::status)
			.containsExactly(ClassSessionStatus.COMPLETED, ClassSessionStatus.IN_PROGRESS,
				ClassSessionStatus.SCHEDULED);
		assertThat(all.totalElements()).isEqualTo(3);

		ClassSessionPageResponse completed = service.list(fixture.managerId(), null, null, fixture.classId(),
			fixture.coachId(), fixture.roomId(), ClassSessionStatus.COMPLETED, page);
		assertThat(completed.content()).singleElement()
			.extracting(ClassSessionResponse::status).isEqualTo(ClassSessionStatus.COMPLETED);
	}

	private ClassSession session(Fixture fixture, Instant start, Instant end) {
		return new ClassSession(UUID.randomUUID(), fixture.sportClass(), null, fixture.coach(), fixture.room(),
			start, end, 12, fixture.managerId());
	}

	private Fixture fixture() {
		Account manager = saveAccount(AccountRole.MANAGER, "Manager");
		Account coach = saveAccount(AccountRole.COACH, "Coach");
		Discipline discipline = disciplines.saveAndFlush(new Discipline(UUID.randomUUID(), "Yoga", null,
			DisciplineStatus.ACTIVE));
		SportClass sportClass = classes.saveAndFlush(new SportClass(UUID.randomUUID(), discipline, "Vinyasa",
			SportClassType.YOGA, null, SportClassStatus.ACTIVE));
		Room room = rooms.saveAndFlush(new Room(UUID.randomUUID(), "Studio A", 20, RoomStatus.ACTIVE));
		return new Fixture(manager.getId(), coach.getId(), sportClass.getId(), room.getId(), sportClass, coach, room);
	}

	private Account saveAccount(AccountRole role, String name) {
		UUID id = UUID.randomUUID();
		return accounts.saveAndFlush(new Account(id, role, AccountStatus.ACTIVE, name,
			"09" + id.toString().replace("-", "").substring(0, 8), id + "@example.test",
			LocalDate.of(1990, 1, 1), "{noop}password"));
	}

	@TestConfiguration
	static class FixedClockConfiguration {
		@Bean
		@Primary
		Clock fixedClock() {
			return Clock.fixed(NOW, ZoneOffset.UTC);
		}
	}

	private record Fixture(UUID managerId, UUID coachId, UUID classId, UUID roomId,
			SportClass sportClass, Account coach, Room room) { }
}
