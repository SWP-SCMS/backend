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
import com.scms.backend.audit.AuditEventRepository;
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
	@Mock AuditEventRepository audits;
	@Mock SessionLifecycleService lifecycle;
	RecurringScheduleService service;

	@BeforeEach
	void setUp() {
		java.time.Clock clock = java.time.Clock.fixed(Instant.parse("2026-10-05T00:00:00Z"), ZoneOffset.UTC);
		service = new RecurringScheduleService(schedules, sessions,
			new ClassSessionService(sessions, classes, rooms, accounts, audits, lifecycle, clock), clock);
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
	void managerCanChooseSessionCount() {
		UUID managerId = manager(AccountStatus.ACTIVE);
		SportClass sportClass = sportClass(DisciplineStatus.ACTIVE, SportClassStatus.ACTIVE);
		Room room = room(RoomStatus.ACTIVE, 20);
		Account coach = account(AccountRole.COACH, AccountStatus.ACTIVE);
		stubActiveResources(sportClass, room, coach);
		when(schedules.saveAndFlush(any(RecurringSchedule.class)))
			.thenAnswer(invocation -> invocation.getArgument(0));
		when(sessions.saveAllAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
		RecurringScheduleCreateRequest request = new RecurringScheduleCreateRequest(sportClass.getId(), coach.getId(),
			room.getId(), LocalDate.of(2026, 10, 6), List.of(2, 4), LocalTime.of(9, 0),
			LocalTime.of(10, 0), 10, 3);

		RecurringScheduleResponse result = service.create(managerId, request);

		assertThat(result.sessionCount()).isEqualTo(3);
		assertThat(result.sessionIds()).hasSize(3);
		ArgumentCaptor<List<ClassSession>> created = ArgumentCaptor.forClass(List.class);
		verify(sessions).saveAllAndFlush(created.capture());
		assertThat(created.getValue()).hasSize(3);
	}

	@Test
	void sessionCountMustBeBetweenOneAndOneHundred() {
		UUID managerId = manager(AccountStatus.ACTIVE);
		for (int sessionCount : List.of(0, 101)) {
			RecurringScheduleCreateRequest request = new RecurringScheduleCreateRequest(UUID.randomUUID(),
				UUID.randomUUID(), UUID.randomUUID(), LocalDate.of(2026, 10, 6), List.of(2),
				LocalTime.of(9, 0), LocalTime.of(10, 0), 10, sessionCount);

			assertThatThrownBy(() -> service.create(managerId, request))
				.isInstanceOf(RecurringScheduleException.class)
				.extracting("field").isEqualTo("sessionCount");
		}
		verify(schedules, never()).saveAndFlush(any());
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
		when(sessions.existsCoachOverlap(any(), any(), any())).thenReturn(false, false, false, true);

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
			.extracting("code").isEqualTo("ROOM_CAPACITY_BELOW_SESSION_CAPACITY");

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

	@Test
	void recurringSessionLongerThanNinetyMinutesIsRejected() {
		UUID managerId = manager(AccountStatus.ACTIVE);

		assertThatThrownBy(() -> service.create(managerId, new RecurringScheduleCreateRequest(
			UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), LocalDate.of(2026, 10, 6), List.of(2),
			LocalTime.of(9, 0), LocalTime.of(10, 31), 10)))
			.isInstanceOf(RecurringScheduleException.class)
			.extracting("field").isEqualTo("endTime");
		verify(schedules, never()).saveAndFlush(any());
	}

	@Test
	void recurringSessionDurationMustBeBetweenThirtyAndNinetyMinutes() {
		UUID managerId = manager(AccountStatus.ACTIVE);
		SportClass sportClass = sportClass(DisciplineStatus.ACTIVE, SportClassStatus.ACTIVE);
		Room room = room(RoomStatus.ACTIVE, 20);
		Account coach = account(AccountRole.COACH, AccountStatus.ACTIVE);
		stubActiveResources(sportClass, room, coach);
		when(schedules.saveAndFlush(any(RecurringSchedule.class)))
			.thenAnswer(invocation -> invocation.getArgument(0));
		when(sessions.saveAllAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

		assertThat(service.create(managerId, new RecurringScheduleCreateRequest(sportClass.getId(), coach.getId(),
			room.getId(), LocalDate.of(2026, 10, 6), List.of(2), LocalTime.of(9, 0), LocalTime.of(9, 30), 10)))
			.isNotNull();
		assertThat(service.create(managerId, new RecurringScheduleCreateRequest(sportClass.getId(), coach.getId(),
			room.getId(), LocalDate.of(2026, 10, 6), List.of(4), LocalTime.of(9, 0), LocalTime.of(10, 30), 10)))
			.isNotNull();

		assertThatThrownBy(() -> service.create(managerId, new RecurringScheduleCreateRequest(sportClass.getId(),
			coach.getId(), room.getId(), LocalDate.of(2026, 10, 6), List.of(6), LocalTime.of(9, 0),
			LocalTime.of(9, 29, 59), 10)))
			.isInstanceOf(RecurringScheduleException.class)
			.satisfies(error -> {
				RecurringScheduleException exception = (RecurringScheduleException) error;
				assertThat(exception.getCode()).isEqualTo("VALIDATION_ERROR");
				assertThat(exception.getField()).isEqualTo("endTime");
			});
		assertThatThrownBy(() -> service.create(managerId, new RecurringScheduleCreateRequest(sportClass.getId(),
			coach.getId(), room.getId(), LocalDate.of(2026, 10, 6), List.of(7), LocalTime.of(9, 0),
			LocalTime.of(10, 30, 0, 1_000_000), 10)))
			.isInstanceOf(RecurringScheduleException.class)
			.satisfies(error -> {
				RecurringScheduleException exception = (RecurringScheduleException) error;
				assertThat(exception.getCode()).isEqualTo("VALIDATION_ERROR");
				assertThat(exception.getField()).isEqualTo("endTime");
			});
	}

	@Test
	void recurringScheduleRejectsLegacyYogaClass() {
		UUID managerId = manager(AccountStatus.ACTIVE);
		Discipline yoga = new Discipline(UUID.randomUUID(), "Yoga", null, DisciplineStatus.ACTIVE);
		SportClass legacyYoga = new SportClass(UUID.randomUUID(), yoga, "Legacy Flow", SportClassType.YOGA,
			null, SportClassStatus.ACTIVE);
		Room room = room(RoomStatus.ACTIVE, 20);
		Account coach = account(AccountRole.COACH, AccountStatus.ACTIVE);
		when(classes.findById(legacyYoga.getId())).thenReturn(Optional.of(legacyYoga));

		assertThatThrownBy(() -> service.create(managerId, request(legacyYoga, room, coach,
			LocalDate.of(2026, 10, 6), List.of(2))))
			.isInstanceOfSatisfying(RecurringScheduleException.class, exception -> {
				assertThat(exception.getCode()).isEqualTo("LEGACY_YOGA_UNSUPPORTED");
				assertThat(exception.getField()).isEqualTo("classId");
			});
		verify(schedules, never()).saveAndFlush(any(RecurringSchedule.class));
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
		Discipline discipline = new Discipline(UUID.randomUUID(), "Pilates", null, disciplineStatus);
		return new SportClass(UUID.randomUUID(), discipline, "Vinyasa", SportClassType.GROUP, null, classStatus);
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
