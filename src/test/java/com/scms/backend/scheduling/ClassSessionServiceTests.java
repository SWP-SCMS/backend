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
	@Mock CoachLeaveRepository coachLeaves;
	ClassSessionService service;

	private final Instant now = Instant.parse("2026-10-05T00:00:00Z");

	@BeforeEach
	void setUp() {
		service = new ClassSessionService(sessions, classes, rooms, accounts, audits, lifecycle,
			Clock.fixed(now, ZoneOffset.UTC), null, coachLeaves, null);
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
	void sessionLongerThanNinetyMinutesIsRejected() {
		UUID managerId = activeManager();

		assertThatThrownBy(() -> service.create(managerId, request(UUID.randomUUID(), UUID.randomUUID(),
			UUID.randomUUID(), now.plusSeconds(3600), now.plusSeconds(3600 + 91 * 60), 10)))
			.isInstanceOf(RecurringScheduleException.class)
			.extracting("field").isEqualTo("endTime");
		verify(classes, never()).findById(any());
	}

	@Test
	void sessionDurationMustBeBetweenThirtyAndNinetyMinutes() {
		Fixture fixture = activeFixture(20);
		when(sessions.existsOverlap(any(), any(), any(), any())).thenReturn(false);
		when(sessions.saveAndFlush(any(ClassSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

		assertThat(service.create(fixture.managerId(), request(fixture.sportClass().getId(), fixture.coach().getId(),
			fixture.room().getId(), now.plusSeconds(3600), now.plusSeconds(3600 + 30 * 60), 10))).isNotNull();
		assertThat(service.create(fixture.managerId(), request(fixture.sportClass().getId(), fixture.coach().getId(),
			fixture.room().getId(), now.plusSeconds(7200), now.plusSeconds(7200 + 90 * 60), 10))).isNotNull();

		assertThatThrownBy(() -> service.create(fixture.managerId(), request(fixture.sportClass().getId(),
			fixture.coach().getId(), fixture.room().getId(), now.plusSeconds(10800),
			now.plusSeconds(10800 + 29 * 60 + 59), 10)))
			.isInstanceOf(RecurringScheduleException.class)
			.satisfies(error -> {
				RecurringScheduleException exception = (RecurringScheduleException) error;
				assertThat(exception.getCode()).isEqualTo("VALIDATION_ERROR");
				assertThat(exception.getField()).isEqualTo("endTime");
			});
		assertThatThrownBy(() -> service.create(fixture.managerId(), request(fixture.sportClass().getId(),
			fixture.coach().getId(), fixture.room().getId(), now.plusSeconds(14400),
			now.plusSeconds(14400 + 90 * 60).plusMillis(1), 10)))
			.isInstanceOf(RecurringScheduleException.class)
			.satisfies(error -> {
				RecurringScheduleException exception = (RecurringScheduleException) error;
				assertThat(exception.getCode()).isEqualTo("VALIDATION_ERROR");
				assertThat(exception.getField()).isEqualTo("endTime");
			});
	}

	@Test
	void createRejectsLegacyYogaClassWhileHistoryRemainsReadable() {
		UUID managerId = activeManager();
		Discipline yoga = new Discipline(UUID.randomUUID(), "Yoga", null, DisciplineStatus.ACTIVE);
		SportClass legacyYoga = new SportClass(UUID.randomUUID(), yoga, "Legacy Flow", SportClassType.YOGA,
			null, SportClassStatus.ACTIVE);
		when(classes.findById(legacyYoga.getId())).thenReturn(Optional.of(legacyYoga));

		assertThatThrownBy(() -> service.create(managerId, request(legacyYoga.getId(),
			UUID.randomUUID(), UUID.randomUUID(), now.plusSeconds(3600), now.plusSeconds(7200), 10)))
			.isInstanceOfSatisfying(RecurringScheduleException.class, exception -> {
				assertThat(exception.getCode()).isEqualTo("LEGACY_YOGA_UNSUPPORTED");
				assertThat(exception.getField()).isEqualTo("classId");
			});
		verify(sessions, never()).saveAndFlush(any(ClassSession.class));
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

	@Test
	void approvedCoachLeaveRejectsNewSession() {
		Fixture fixture = activeFixture(20);
		Instant start = now.plusSeconds(3600);
		Instant end = now.plusSeconds(7200);
		when(sessions.existsOverlap(fixture.coach().getId(), fixture.room().getId(), start, end)).thenReturn(false);
		when(coachLeaves.existsApprovedOverlap(fixture.coach().getId(), start, end)).thenReturn(true);

		assertThatThrownBy(() -> service.create(fixture.managerId(), request(fixture.sportClass().getId(),
			fixture.coach().getId(), fixture.room().getId(), start, end, 10)))
			.isInstanceOf(RecurringScheduleException.class)
			.extracting("code").isEqualTo("COACH_ON_APPROVED_LEAVE");
		verify(sessions, never()).saveAndFlush(any());
	}

	@Test
	void managerCreatesRoomlessPtSlotWithCapacityOne() {
		UUID managerId = activeManager();
		Discipline discipline = new Discipline(UUID.randomUUID(), "PT", null, DisciplineStatus.ACTIVE);
		SportClass sportClass = new SportClass(UUID.randomUUID(), discipline, "One to one", SportClassType.PT_1_1,
			null, SportClassStatus.ACTIVE);
		Account coach = account(AccountRole.COACH, AccountStatus.ACTIVE);
		when(classes.findById(sportClass.getId())).thenReturn(Optional.of(sportClass));
		when(accounts.findById(coach.getId())).thenReturn(Optional.of(coach));
		when(sessions.existsOverlap(coach.getId(), null, now.plusSeconds(3600), now.plusSeconds(7200))).thenReturn(false);
		when(sessions.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

		ClassSessionResponse response = service.create(managerId, new ClassSessionCreateRequest(sportClass.getId(),
			coach.getId(), null, now.plusSeconds(3600), now.plusSeconds(7200), 1));
		assertThat(response.roomId()).isNull();
		assertThat(response.capacity()).isEqualTo(1);
	}

	@Test
	void ptCapacityOtherThanOneIsRejected() {
		UUID managerId = activeManager();
		Discipline discipline = new Discipline(UUID.randomUUID(), "PT", null, DisciplineStatus.ACTIVE);
		SportClass sportClass = new SportClass(UUID.randomUUID(), discipline, "One to one", SportClassType.PT_1_1,
			null, SportClassStatus.ACTIVE);
		when(classes.findById(sportClass.getId())).thenReturn(Optional.of(sportClass));
		assertThatThrownBy(() -> service.create(managerId, new ClassSessionCreateRequest(sportClass.getId(),
			UUID.randomUUID(), null, now.plusSeconds(3600), now.plusSeconds(7200), 2)))
			.isInstanceOf(RecurringScheduleException.class)
			.extracting("code").isEqualTo("PT_CAPACITY_MUST_BE_ONE");
	}

	@Test
	void roomImpactRescheduleKeepsSessionAndBookingsAndUsesConflictChecks() {
		UUID managerId = activeManager();
		Discipline discipline = new Discipline(UUID.randomUUID(), "Impact Discipline", null, DisciplineStatus.ACTIVE);
		SportClass sportClass = new SportClass(UUID.randomUUID(), discipline, "Impact Class", SportClassType.GROUP,
			null, SportClassStatus.ACTIVE);
		Room room = new Room(UUID.randomUUID(), "Impact Room", 20, RoomStatus.ACTIVE);
		Account coach = account(AccountRole.COACH, AccountStatus.ACTIVE);
		ClassSession session = new ClassSession(UUID.randomUUID(), sportClass, null, coach,
			room, now.plusSeconds(3600), now.plusSeconds(5400), 10, managerId);
		Instant newStart = now.plusSeconds(7200);
		Instant newEnd = now.plusSeconds(9000);
		when(sessions.existsOverlapExcluding(session.getId(), coach.getId(), room.getId(),
			newStart, newEnd)).thenReturn(false);
		when(rooms.existsSuspensionOverlap(room.getId(), newStart, newEnd)).thenReturn(false);
		when(sessions.saveAndFlush(any(ClassSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

		service.rescheduleForRoomImpact(managerId, session, newStart, newEnd, "Room repair");

		assertThat(session.getStartTime()).isEqualTo(newStart);
		assertThat(session.getEndTime()).isEqualTo(newEnd);
		verify(sessions).existsOverlapExcluding(session.getId(), coach.getId(), room.getId(),
			newStart, newEnd);
	}

	private Fixture activeFixture(int roomCapacity) {
		UUID managerId = activeManager();
		Discipline discipline = new Discipline(UUID.randomUUID(), "Pilates", null, DisciplineStatus.ACTIVE);
		SportClass sportClass = new SportClass(UUID.randomUUID(), discipline, "Vinyasa", SportClassType.GROUP,
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
