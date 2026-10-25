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
import java.util.List;
import java.util.UUID;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.auth.InvalidAuthenticatedAccountException;
import com.scms.backend.audit.AuditEventRepository;
import com.scms.backend.notification.NotificationWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ClassSessionServiceTests {

	@Mock ClassSessionRepository sessions;
	@Mock SportClassRepository classes;
	@Mock RoomRepository rooms;
	@Mock AccountRepository accounts;
	@Mock AuditEventRepository audits;
	@Mock SessionLifecycleService lifecycle;
	@Mock CoachLeaveRepository coachLeaves;
	@Mock NotificationWriter notifications;
	ClassSessionService service;

	private final Instant now = Instant.parse("2026-10-05T00:00:00Z");

	@BeforeEach
	void setUp() {
		service = new ClassSessionService(sessions, classes, rooms, accounts, audits, lifecycle,
			Clock.fixed(now, ZoneOffset.UTC), null, coachLeaves, null, notifications, null, null);
	}

	@Test
	void activeManagerCreatesScheduledSingleSession() {
		Fixture fixture = activeFixture(20);
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
			.extracting("code").isEqualTo("ROOM_CAPACITY_BELOW_SESSION_CAPACITY");
		verify(sessions, never()).saveAndFlush(any());
	}

	@Test
	void overlapRejectsSingleSessionBeforeWrite() {
		Fixture fixture = activeFixture(20);
		when(sessions.existsCoachOverlap(fixture.coach().getId(), now.plusSeconds(3600),
			now.plusSeconds(7200))).thenReturn(true);

		assertThatThrownBy(() -> service.create(fixture.managerId(), request(fixture.sportClass().getId(),
			fixture.coach().getId(), fixture.room().getId(), now.plusSeconds(3600), now.plusSeconds(7200), 10)))
			.isInstanceOf(RecurringScheduleException.class)
			.extracting("code").isEqualTo("SESSION_CONFLICT");
		verify(sessions, never()).saveAndFlush(any());
	}

	@Test
	void sharedRoomAllowsOverlapWhenAggregateCapacityFits() {
		Fixture fixture = activeFixture(10);
		fixture.room().update(fixture.room().getName(), 10, RoomStatus.ACTIVE, RoomUsageMode.SHARED);
		Instant start = now.plusSeconds(3600);
		Instant end = now.plusSeconds(7200);
		when(sessions.sumOverlappingCapacity(fixture.room().getId(), start, end)).thenReturn(6L);
		when(sessions.saveAndFlush(any(ClassSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

		ClassSessionResponse response = service.create(fixture.managerId(), request(fixture.sportClass().getId(),
			fixture.coach().getId(), fixture.room().getId(), start, end, 4));

		assertThat(response.capacity()).isEqualTo(4);
	}

	@Test
	void sharedRoomRejectsOverlapWhenAggregateCapacityWouldBeExceeded() {
		Fixture fixture = activeFixture(10);
		fixture.room().update(fixture.room().getName(), 10, RoomStatus.ACTIVE, RoomUsageMode.SHARED);
		Instant start = now.plusSeconds(3600);
		Instant end = now.plusSeconds(7200);
		when(sessions.sumOverlappingCapacity(fixture.room().getId(), start, end)).thenReturn(6L);

		assertThatThrownBy(() -> service.create(fixture.managerId(), request(fixture.sportClass().getId(),
			fixture.coach().getId(), fixture.room().getId(), start, end, 5)))
			.isInstanceOfSatisfying(RecurringScheduleException.class, error -> {
				assertThat(error.getCode()).isEqualTo("SHARED_ROOM_CAPACITY_EXCEEDED");
				assertThat(error.getRoomCapacity()).isEqualTo(10);
				assertThat(error.getOccupiedCapacity()).isEqualTo(6);
				assertThat(error.getRequestedCapacity()).isEqualTo(5);
			});
		verify(sessions, never()).saveAndFlush(any());
	}

	@Test
	void exclusiveRoomRejectsOverlappingSessionWithSpecificCode() {
		Fixture fixture = activeFixture(20);
		Instant start = now.plusSeconds(3600);
		Instant end = now.plusSeconds(7200);
		when(sessions.existsRoomOverlap(fixture.room().getId(), start, end)).thenReturn(true);

		assertThatThrownBy(() -> service.create(fixture.managerId(), request(fixture.sportClass().getId(),
			fixture.coach().getId(), fixture.room().getId(), start, end, 10)))
			.isInstanceOf(RecurringScheduleException.class)
			.extracting("code").isEqualTo("ROOM_TIME_CONFLICT");
	}

	@Test
	void approvedCoachLeaveRejectsNewSession() {
		Fixture fixture = activeFixture(20);
		Instant start = now.plusSeconds(3600);
		Instant end = now.plusSeconds(7200);
		when(coachLeaves.existsApprovedOverlap(fixture.coach().getId(), start, end)).thenReturn(true);

		assertThatThrownBy(() -> service.create(fixture.managerId(), request(fixture.sportClass().getId(),
			fixture.coach().getId(), fixture.room().getId(), start, end, 10)))
			.isInstanceOf(RecurringScheduleException.class)
			.extracting("code").isEqualTo("COACH_ON_APPROVED_LEAVE");
		verify(sessions, never()).saveAndFlush(any());
	}

	@Test
	void availableCoachesUsesTheSessionIntervalAndExcludesTheSessionItself() {
		UUID managerId = activeManager();
		ClassSession session = sessionAt(now.plusSeconds(3600));
		UUID coachId = UUID.randomUUID();
		ClassSessionRepository.AvailableCoachView view = org.mockito.Mockito.mock(
			ClassSessionRepository.AvailableCoachView.class);
		when(view.getCoachAccountId()).thenReturn(coachId);
		when(view.getFullName()).thenReturn("Available Coach");
		when(view.getPhone()).thenReturn("0901234567");
		when(sessions.findById(session.getId())).thenReturn(Optional.of(session));
		when(sessions.findAvailableCoaches(session.getId(), session.getStartTime(), session.getEndTime()))
			.thenReturn(List.of(view));

		List<AvailableCoachResponse> result = service.availableCoaches(managerId, session.getId());

		assertThat(result).containsExactly(new AvailableCoachResponse(coachId, "Available Coach", "0901234567"));
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
	void managerReschedulesAtomicallyAndNotifiesBookedMembers() {
		UUID managerId = activeManager(); ClassSession session = sessionAt(now.plusSeconds(48 * 3600));
		ReflectionTestUtils.setField(session, "version", 0L);
		Instant newStart = now.plusSeconds(72 * 3600); Instant newEnd = newStart.plusSeconds(3600);
		when(sessions.findByIdForUpdate(session.getId())).thenReturn(Optional.of(session));
		when(classes.findById(session.getSportClass().getId())).thenReturn(Optional.of(session.getSportClass()));
		when(accounts.findById(session.getTeachingCoach().getId())).thenReturn(Optional.of(session.getTeachingCoach()));
		when(rooms.findById(session.getRoom().getId())).thenReturn(Optional.of(session.getRoom()));
		when(sessions.findBookedMemberConflictIds(session.getId(), newStart, newEnd)).thenReturn(java.util.List.of());
		UUID memberId = UUID.randomUUID(); when(sessions.findBookedMemberIds(session.getId())).thenReturn(java.util.List.of(memberId));
		when(sessions.saveAndFlush(session)).thenReturn(session);

		ClassSessionDetailResponse response = service.reschedule(managerId, session.getId(),
			new ClassSessionScheduleRequest(newStart, newEnd, null, null, 0L, "Coach requested change"));

		assertThat(response.startTime()).isEqualTo(newStart);
		verify(audits).save(any());
		verify(notifications).write(any(), org.mockito.ArgumentMatchers.eq(memberId),
			org.mockito.ArgumentMatchers.eq("SESSION_RESCHEDULED"), any(), any(), any());
	}

	@Test
	void rescheduleInsideTwentyFourHoursIsRejected() {
		UUID managerId = activeManager(); ClassSession session = sessionAt(now.plusSeconds(23 * 3600));
		ReflectionTestUtils.setField(session, "version", 0L);
		when(sessions.findByIdForUpdate(session.getId())).thenReturn(Optional.of(session));
		assertThatThrownBy(() -> service.reschedule(managerId, session.getId(), new ClassSessionScheduleRequest(
			now.plusSeconds(48 * 3600), now.plusSeconds(49 * 3600), null, null, 0L, "Change")))
			.isInstanceOf(RecurringScheduleException.class)
			.extracting("code").isEqualTo("SESSION_RESCHEDULE_WINDOW_CLOSED");
	}

	@Test
	void rescheduleRejectsBookedMemberOverlapAtNewTime() {
		UUID managerId = activeManager(); ClassSession session = sessionAt(now.plusSeconds(48 * 3600));
		ReflectionTestUtils.setField(session, "version", 0L);
		Instant newStart = now.plusSeconds(72 * 3600); Instant newEnd = newStart.plusSeconds(3600);
		when(sessions.findByIdForUpdate(session.getId())).thenReturn(Optional.of(session));
		when(classes.findById(session.getSportClass().getId())).thenReturn(Optional.of(session.getSportClass()));
		when(accounts.findById(session.getTeachingCoach().getId())).thenReturn(Optional.of(session.getTeachingCoach()));
		when(rooms.findById(session.getRoom().getId())).thenReturn(Optional.of(session.getRoom()));
		when(sessions.findBookedMemberConflictIds(session.getId(), newStart, newEnd)).thenReturn(java.util.List.of(UUID.randomUUID()));
		assertThatThrownBy(() -> service.reschedule(managerId, session.getId(),
			new ClassSessionScheduleRequest(newStart, newEnd, null, null, 0L, "Change")))
			.isInstanceOf(RecurringScheduleException.class)
			.extracting("code").isEqualTo("SESSION_MEMBER_TIME_CONFLICT");
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
		when(rooms.existsSuspensionOverlap(room.getId(), newStart, newEnd)).thenReturn(false);
		when(sessions.saveAndFlush(any(ClassSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

		service.rescheduleForRoomImpact(managerId, session, newStart, newEnd, "Room repair");

		assertThat(session.getStartTime()).isEqualTo(newStart);
		assertThat(session.getEndTime()).isEqualTo(newEnd);
		verify(sessions).existsCoachOverlapExcluding(session.getId(), coach.getId(), newStart, newEnd);
		verify(sessions).existsRoomOverlapExcluding(session.getId(), room.getId(), newStart, newEnd);
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

	private ClassSession sessionAt(Instant start) {
		Discipline discipline = new Discipline(UUID.randomUUID(), "Pilates", null, DisciplineStatus.ACTIVE);
		SportClass sportClass = new SportClass(UUID.randomUUID(), discipline, "Reformer", SportClassType.GROUP,
			null, SportClassStatus.ACTIVE);
		return new ClassSession(UUID.randomUUID(), sportClass, null, account(AccountRole.COACH, AccountStatus.ACTIVE),
			new Room(UUID.randomUUID(), "Studio", 20, RoomStatus.ACTIVE), start, start.plusSeconds(3600), 10,
			UUID.randomUUID());
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
