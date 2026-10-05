package com.scms.backend.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.auth.InvalidAuthenticatedAccountException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RecurringScheduleServiceTests {

	@Mock RecurringScheduleRepository schedules;
	@Mock ClassSessionRepository sessions;
	@Mock SportClassRepository classes;
	@Mock RoomRepository rooms;
	@Mock AccountRepository accounts;
	RecurringScheduleService service;

	@BeforeEach
	void setUp() {
		service = new RecurringScheduleService(schedules, sessions, classes, rooms, accounts,
			java.time.Clock.fixed(Instant.parse("2026-10-05T00:00:00Z"), ZoneOffset.UTC));
	}

	@Test
	void activeManagerCreatesThirtySessionsOnRequestedWeekdays() {
		UUID managerId = manager(AccountStatus.ACTIVE);
		SportClass sportClass = sportClass(DisciplineStatus.ACTIVE, SportClassStatus.ACTIVE);
		Room room = room(RoomStatus.ACTIVE, 20);
		Account coach = account(AccountRole.COACH, AccountStatus.ACTIVE);
		stubActiveResources(sportClass, room, coach);
		when(schedules.saveAndFlush(any(RecurringSchedule.class)))
			.thenAnswer(invocation -> invocation.getArgument(0));
		when(sessions.saveAllAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

		RecurringScheduleResponse result = service.create(managerId, request(sportClass, room, coach,
			LocalDate.of(2026, 10, 6), List.of(2, 4)));

		assertThat(result.sessionCount()).isEqualTo(30);
		ArgumentCaptor<List<ClassSession>> created = ArgumentCaptor.forClass(List.class);
		verify(sessions).saveAllAndFlush(created.capture());
		assertThat(created.getValue()).hasSize(30);
		assertThat(created.getValue()).allSatisfy(session -> {
			assertThat(session.getSportClass()).isSameAs(sportClass);
			assertThat(session.getRoom()).isSameAs(room);
			assertThat(session.getTeachingCoach()).isSameAs(coach);
			assertThat(session.getCreatedBy()).isEqualTo(managerId);
			assertThat(session.getStatus()).isEqualTo(ClassSessionStatus.SCHEDULED);
		});
		assertThat(created.getValue()).extracting(session -> session.getStartTime().atOffset(ZoneOffset.UTC))
			.contains(Instant.parse("2026-10-06T02:00:00Z").atOffset(ZoneOffset.UTC));
		assertThat(created.getValue()).extracting(session -> session.getStartTime().atOffset(ZoneOffset.UTC))
			.allSatisfy(start -> assertThat(start.getHour()).isEqualTo(2));
	}

	@Test
	void inactiveManagerCannotCreateSchedule() {
		UUID managerId = manager(AccountStatus.INACTIVE);

		assertThatThrownBy(() -> service.create(managerId, request(UUID.randomUUID(), UUID.randomUUID(),
			UUID.randomUUID(), LocalDate.of(2026, 10, 6), List.of(1))))
			.isInstanceOf(InvalidAuthenticatedAccountException.class);
		verify(schedules, never()).saveAndFlush(any());
	}

	@Test
	void duplicateWeekdaysAreRejectedBeforeWrites() {
		UUID managerId = manager(AccountStatus.ACTIVE);

		assertThatThrownBy(() -> service.create(managerId, request(UUID.randomUUID(), UUID.randomUUID(),
			UUID.randomUUID(), LocalDate.of(2026, 10, 6), List.of(1, 1))))
			.isInstanceOf(RecurringScheduleException.class)
			.extracting("field").isEqualTo("weekdays");
		verify(schedules, never()).saveAndFlush(any());
	}

	@Test
	void overlapInAnyOccurrenceRejectsWholeScheduleBeforeWrites() {
		UUID managerId = manager(AccountStatus.ACTIVE);
		SportClass sportClass = sportClass(DisciplineStatus.ACTIVE, SportClassStatus.ACTIVE);
		Room room = room(RoomStatus.ACTIVE, 20);
		Account coach = account(AccountRole.COACH, AccountStatus.ACTIVE);
		stubActiveResources(sportClass, room, coach);
		when(sessions.existsOverlap(any(), any(), any(), any())).thenReturn(false, false, false, true);

		assertThatThrownBy(() -> service.create(managerId, request(sportClass, room, coach,
			LocalDate.of(2026, 10, 6), List.of(1))))
			.isInstanceOf(RecurringScheduleException.class)
			.extracting("code").isEqualTo("SESSION_CONFLICT");
		verify(schedules, never()).saveAndFlush(any());
		verify(sessions, never()).saveAllAndFlush(any());
	}

	@Test
	void roomCapacityAndInvalidTimesAreRejectedBeforeWrites() {
		UUID managerId = manager(AccountStatus.ACTIVE);
		SportClass sportClass = sportClass(DisciplineStatus.ACTIVE, SportClassStatus.ACTIVE);
		Room room = room(RoomStatus.ACTIVE, 10);
		Account coach = account(AccountRole.COACH, AccountStatus.ACTIVE);
		stubActiveResources(sportClass, room, coach);

		assertThatThrownBy(() -> service.create(managerId, new RecurringScheduleCreateRequest(
			sportClass.getId(), coach.getId(), room.getId(), LocalDate.of(2026, 10, 6), List.of(1),
			LocalTime.of(10, 0), LocalTime.of(11, 0), 11)))
			.isInstanceOf(RecurringScheduleException.class)
			.extracting("code").isEqualTo("SESSION_CAPACITY_EXCEEDS_ROOM");

		assertThatThrownBy(() -> service.create(managerId, new RecurringScheduleCreateRequest(
			sportClass.getId(), coach.getId(), room.getId(), LocalDate.of(2026, 10, 6), List.of(1),
			LocalTime.of(11, 0), LocalTime.of(10, 0), 10)))
			.isInstanceOf(RecurringScheduleException.class)
			.extracting("field").isEqualTo("endTime");
		verify(schedules, never()).saveAndFlush(any());
	}

	@Test
	void firstOccurrenceMustBeInTheFuture() {
		UUID managerId = manager(AccountStatus.ACTIVE);
		SportClass sportClass = sportClass(DisciplineStatus.ACTIVE, SportClassStatus.ACTIVE);
		Room room = room(RoomStatus.ACTIVE, 10);
		Account coach = account(AccountRole.COACH, AccountStatus.ACTIVE);
		stubActiveResources(sportClass, room, coach);

		assertThatThrownBy(() -> service.create(managerId, new RecurringScheduleCreateRequest(
			sportClass.getId(), coach.getId(), room.getId(), LocalDate.of(2026, 10, 5), List.of(1),
			LocalTime.of(6, 0), LocalTime.of(7, 0), 10)))
			.isInstanceOf(RecurringScheduleException.class)
			.extracting("field").isEqualTo("startTime");
		verify(schedules, never()).saveAndFlush(any());
	}

	private void stubActiveResources(SportClass sportClass, Room room, Account coach) {
		when(classes.findById(sportClass.getId())).thenReturn(Optional.of(sportClass));
		when(rooms.findById(room.getId())).thenReturn(Optional.of(room));
		when(accounts.findById(coach.getId())).thenReturn(Optional.of(coach));
	}

	private UUID manager(AccountStatus status) {
		UUID id = UUID.randomUUID();
		when(accounts.existsByIdAndRoleAndStatus(id, AccountRole.MANAGER, AccountStatus.ACTIVE))
			.thenReturn(status == AccountStatus.ACTIVE);
		return id;
	}

	private Account account(AccountRole role, AccountStatus status) {
		UUID id = UUID.randomUUID();
		return new Account(id, role, status, "Coach", "090" + id.toString().substring(0, 8),
			id + "@example.test", LocalDate.of(1990, 1, 1), "{noop}password");
	}

	private SportClass sportClass(DisciplineStatus disciplineStatus, SportClassStatus classStatus) {
		Discipline discipline = new Discipline(UUID.randomUUID(), "Yoga", null, disciplineStatus);
		return new SportClass(UUID.randomUUID(), discipline, "Vinyasa", SportClassType.YOGA, null, classStatus);
	}

	private Room room(RoomStatus status, int capacity) {
		return new Room(UUID.randomUUID(), "Studio", capacity, status);
	}

	private RecurringScheduleCreateRequest request(SportClass sportClass, Room room, Account coach,
			LocalDate startDate, List<Integer> weekdays) {
		return request(sportClass.getId(), room.getId(), coach.getId(), startDate, weekdays);
	}

	private RecurringScheduleCreateRequest request(UUID classId, UUID roomId, UUID coachId,
			LocalDate startDate, List<Integer> weekdays) {
		return new RecurringScheduleCreateRequest(classId, coachId, roomId, startDate, weekdays,
			LocalTime.of(9, 0), LocalTime.of(10, 0), 10);
	}
}
