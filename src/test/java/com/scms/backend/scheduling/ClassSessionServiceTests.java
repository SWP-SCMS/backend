package com.scms.backend.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.auth.InvalidAuthenticatedAccountException;
import com.scms.backend.audit.AuditEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClassSessionServiceTests {

	@Mock ClassSessionRepository sessions;
	@Mock SportClassRepository classes;
	@Mock RoomRepository rooms;
	@Mock AccountRepository accounts;
	@Mock AuditEventRepository audits;
	@Mock SessionLifecycleService lifecycle;
	ClassSessionService service;

	private final Instant now = Instant.parse("2026-10-05T00:00:00Z");

	@BeforeEach
	void setUp() {
		service = new ClassSessionService(sessions, classes, rooms, accounts, audits, lifecycle,
			Clock.fixed(now, ZoneOffset.UTC));
	}

	@Test
	void activeManagerCreatesScheduledSingleSession() {
		Fixture fixture = activeFixture(20);
		when(sessions.existsOverlap(fixture.coach().getId(), fixture.room().getId(),
			now.plusSeconds(3600), now.plusSeconds(7200))).thenReturn(false);
		when(sessions.saveAndFlush(any(ClassSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

		ClassSessionResponse result = service.create(fixture.managerId(), new ClassSessionCreateRequest(
			fixture.sportClass().getId(), fixture.coach().getId(), fixture.room().getId(),
			now.plusSeconds(3600), now.plusSeconds(7200), 15));

		assertThat(result.classId()).isEqualTo(fixture.sportClass().getId());
		assertThat(result.coachId()).isEqualTo(fixture.coach().getId());
		assertThat(result.roomId()).isEqualTo(fixture.room().getId());
		assertThat(result.capacity()).isEqualTo(15);
		assertThat(result.status()).isEqualTo(ClassSessionStatus.SCHEDULED);
	}

	@Test
	void inactiveManagerCannotCreateSingleSession() {
		UUID managerId = UUID.randomUUID();
		when(accounts.existsByIdAndRoleAndStatus(managerId, AccountRole.MANAGER, AccountStatus.ACTIVE))
			.thenReturn(false);

		assertThatThrownBy(() -> service.create(managerId, request(UUID.randomUUID(), UUID.randomUUID(),
			UUID.randomUUID(), now.plusSeconds(3600), now.plusSeconds(7200), 10)))
			.isInstanceOf(InvalidAuthenticatedAccountException.class);
		verify(sessions, never()).saveAndFlush(any());
	}

	@Test
	void equalOrPastTimesAreRejectedBeforeResourceReads() {
		UUID managerId = activeManager();

		assertThatThrownBy(() -> service.create(managerId, request(UUID.randomUUID(), UUID.randomUUID(),
			UUID.randomUUID(), now.plusSeconds(3600), now.plusSeconds(3600), 10)))
			.isInstanceOf(RecurringScheduleException.class)
			.extracting("field").isEqualTo("endTime");
		assertThatThrownBy(() -> service.create(managerId, request(UUID.randomUUID(), UUID.randomUUID(),
			UUID.randomUUID(), now.minusSeconds(1), now.plusSeconds(3600), 10)))
			.isInstanceOf(RecurringScheduleException.class)
			.extracting("field").isEqualTo("startTime");
		verify(classes, never()).findById(any());
	}

	@Test
	void capacityCannotExceedActiveRoom() {
		Fixture fixture = activeFixture(10);

		assertThatThrownBy(() -> service.create(fixture.managerId(), request(fixture.sportClass().getId(),
			fixture.coach().getId(), fixture.room().getId(), now.plusSeconds(3600), now.plusSeconds(7200), 11)))
			.isInstanceOf(RecurringScheduleException.class)
			.extracting("code").isEqualTo("SESSION_CAPACITY_EXCEEDS_ROOM");
		verify(sessions, never()).saveAndFlush(any());
	}

	@Test
	void overlapRejectsSingleSessionBeforeWrite() {
		Fixture fixture = activeFixture(20);
		when(sessions.existsOverlap(fixture.coach().getId(), fixture.room().getId(),
			now.plusSeconds(3600), now.plusSeconds(7200))).thenReturn(true);

		assertThatThrownBy(() -> service.create(fixture.managerId(), request(fixture.sportClass().getId(),
			fixture.coach().getId(), fixture.room().getId(), now.plusSeconds(3600), now.plusSeconds(7200), 10)))
			.isInstanceOf(RecurringScheduleException.class)
			.extracting("code").isEqualTo("SESSION_CONFLICT");
		verify(sessions, never()).saveAndFlush(any());
	}

	private Fixture activeFixture(int roomCapacity) {
		UUID managerId = activeManager();
		Discipline discipline = new Discipline(UUID.randomUUID(), "Yoga", null, DisciplineStatus.ACTIVE);
		SportClass sportClass = new SportClass(UUID.randomUUID(), discipline, "Vinyasa", SportClassType.YOGA,
			null, SportClassStatus.ACTIVE);
		Room room = new Room(UUID.randomUUID(), "Studio", roomCapacity, RoomStatus.ACTIVE);
		Account coach = account(AccountRole.COACH, AccountStatus.ACTIVE);
		when(classes.findById(sportClass.getId())).thenReturn(Optional.of(sportClass));
		when(rooms.findById(room.getId())).thenReturn(Optional.of(room));
		when(accounts.findById(coach.getId())).thenReturn(Optional.of(coach));
		return new Fixture(managerId, sportClass, room, coach);
	}

	private UUID activeManager() {
		UUID id = UUID.randomUUID();
		when(accounts.existsByIdAndRoleAndStatus(id, AccountRole.MANAGER, AccountStatus.ACTIVE)).thenReturn(true);
		return id;
	}

	private Account account(AccountRole role, AccountStatus status) {
		UUID id = UUID.randomUUID();
		return new Account(id, role, status, "Coach", "090" + id.toString().substring(0, 8),
			id + "@example.test", LocalDate.of(1990, 1, 1), "{noop}password");
	}

	private ClassSessionCreateRequest request(UUID classId, UUID coachId, UUID roomId,
			Instant start, Instant end, int capacity) {
		return new ClassSessionCreateRequest(classId, coachId, roomId, start, end, capacity);
	}

	private record Fixture(UUID managerId, SportClass sportClass, Room room, Account coach) { }
}
