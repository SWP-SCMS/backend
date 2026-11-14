package com.scms.backend.course.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.course.CourseException;
import com.scms.backend.course.CourseRun;
import com.scms.backend.course.CourseSession;
import com.scms.backend.course.run.CourseRunRepository;
import com.scms.backend.course.schedulechange.CourseScheduleChangeService;
import com.scms.backend.scheduling.Room;
import com.scms.backend.scheduling.RoomStatus;
import com.scms.backend.scheduling.coachleave.CoachLeaveRepository;
import com.scms.backend.scheduling.resource.SharedResourceLockService;
import com.scms.backend.scheduling.room.RoomRepository;
import com.scms.backend.scheduling.session.ClassSessionRepository;
import java.math.BigInteger;
import java.time.Instant;
import java.util.Optional;
import java.util.List;
import com.scms.backend.course.CourseSessionStatus;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class CourseSessionServiceTests {

	private final CourseRunRepository runs = mock(CourseRunRepository.class);

	private final CourseSessionRepository sessions = mock(CourseSessionRepository.class);

	private final AccountRepository accounts = mock(AccountRepository.class);

	private final RoomRepository rooms = mock(RoomRepository.class);

	private final CoachLeaveRepository leaves = mock(CoachLeaveRepository.class);

	private final ClassSessionRepository gymSessions = mock(ClassSessionRepository.class);

	private final SharedResourceLockService locks = mock(SharedResourceLockService.class);

	private final CourseScheduleChangeService scheduleChanges = mock(CourseScheduleChangeService.class);

	private CourseSessionService service;

	private CourseRun run;

	private Account coach;

	private Room room;

	private UUID runId;

	private UUID coachId;

	private UUID roomId;

	@BeforeEach
	void setUp() {
		service = new CourseSessionService(runs, sessions, accounts, rooms, leaves, gymSessions, locks, scheduleChanges);
		runId = UUID.randomUUID();
		coachId = UUID.randomUUID();
		roomId = UUID.randomUUID();
		run = mock(CourseRun.class);
		coach = mock(Account.class);
		room = mock(Room.class);
		when(runs.findByIdForUpdate(runId)).thenReturn(Optional.of(run));
		when(accounts.findById(coachId)).thenReturn(Optional.of(coach));
		when(rooms.findById(roomId)).thenReturn(Optional.of(room));
		when(run.getCapacity()).thenReturn(20);
		when(run.getTotalUnits()).thenReturn(12);
		when(coach.getId()).thenReturn(coachId);
		when(coach.getRole()).thenReturn(AccountRole.COACH);
		when(coach.getStatus()).thenReturn(AccountStatus.ACTIVE);
		when(room.getId()).thenReturn(roomId);
		when(room.getCapacity()).thenReturn(20);
		when(room.getStatus()).thenReturn(RoomStatus.ACTIVE);
	}

	@Test
	void rejectsGymConflictForCourseSession() {
		Instant start = Instant.parse("2035-01-01T02:00:00Z");
		Instant end = Instant.parse("2035-01-01T03:00:00Z");
		when(gymSessions.existsOverlap(coachId, roomId, start, end)).thenReturn(true);
		assertThatThrownBy(() -> service.create(runId, 1, coachId, roomId, start, end, BigInteger.TEN)).isInstanceOf(CourseException.class).hasMessageContaining("unavailable");
	}

	@Test
	void rejectsRoomSmallerThanRunCapacity() {
		when(room.getCapacity()).thenReturn(19);
		Instant start = Instant.parse("2035-01-01T02:00:00Z");
		assertThatThrownBy(() -> service.create(runId, 1, coachId, roomId, start, start.plusSeconds(3600), BigInteger.TEN)).isInstanceOf(CourseException.class).hasMessageContaining("capacity");
	}

	@Test
	void rejectsUnitBeyondRunAndAllocationBeyondVndPrecision() {
		Instant start = Instant.parse("2035-01-01T02:00:00Z");
		assertThatThrownBy(() -> service.create(runId, 13, coachId, roomId, start, start.plusSeconds(3600), BigInteger.TEN)).isInstanceOf(CourseException.class);
		assertThatThrownBy(() -> service.create(runId, 1, coachId, roomId, start, start.plusSeconds(3600), new BigInteger("10000000000000000000"))).isInstanceOf(CourseException.class);
	}

	@Test
	void rejectsStaleSessionVersionBeforeLoadingReplacementResources() {
		Instant start = Instant.parse("2035-01-01T02:00:00Z");
		CourseSession session = new CourseSession(UUID.randomUUID(), run, 1, room, coach, start, start.plusSeconds(3600), BigInteger.TEN);
		ReflectionTestUtils.setField(session, "version", 3L);
		when(sessions.findRoomIdById(session.getId())).thenReturn(Optional.of(roomId));
		when(sessions.findCoachIdById(session.getId())).thenReturn(Optional.of(coachId));
		when(sessions.findCourseRunIdById(session.getId())).thenReturn(Optional.of(runId));
		when(runs.findByIdForUpdate(runId)).thenReturn(Optional.of(run));
		when(sessions.findByIdForUpdate(session.getId())).thenReturn(Optional.of(session));
		assertThatThrownBy(() -> service.reschedule(session.getId(), null, null, start.plusSeconds(60), start.plusSeconds(3660), 2L)).isInstanceOfSatisfying(CourseException.class, exception -> assertThat(exception.getCode()).isEqualTo("COURSE_SESSION_CHANGED"));
		verifyNoInteractions(rooms, accounts);
	}
	@Test
	void movingFirstSessionEarlierExpandsRunStart() {
		Instant first = Instant.parse("2035-01-10T12:00:00Z"), last = first.plusSeconds(7 * 86400);
		CourseSession session = prepareReschedule(first, last);
		service.reschedule(session.getId(), null, null, first.minusSeconds(3600), first);
		verify(run).changeSchedule(first.minusSeconds(3600), last.plusSeconds(3600), 12);
		verify(runs).saveAndFlush(run);
	}

	@Test
	void movingFirstSessionAfterAnotherUnitUsesActualEarliestTime() {
		Instant first = Instant.parse("2035-01-10T12:00:00Z"), last = first.plusSeconds(7 * 86400);
		CourseSession session = prepareReschedule(first, last);
		service.reschedule(session.getId(), null, null, last.plusSeconds(86400), last.plusSeconds(90000));
		verify(run).changeSchedule(last, last.plusSeconds(90000), 12);
	}

	@Test
	void movingLastSessionEarlierShrinksRunEnd() {
		Instant first = Instant.parse("2035-01-10T12:00:00Z"), last = first.plusSeconds(7 * 86400);
		CourseSession session = prepareReschedule(last, first);
		when(run.getPlannedStartAt()).thenReturn(first);
		when(run.getPlannedEndAt()).thenReturn(last.plusSeconds(3600));
		service.reschedule(session.getId(), null, null, last.minusSeconds(3600), last);
		verify(run).changeSchedule(first, last, 12);
	}

	@Test
	void movingMiddleSessionInsideExistingBoundsDoesNotRewriteRun() {
		Instant first = Instant.parse("2035-01-10T12:00:00Z"), last = first.plusSeconds(14 * 86400);
		CourseSession session = prepareReschedule(first.plusSeconds(7 * 86400), last);
		CourseSession earliest = new CourseSession(UUID.randomUUID(), run, 3, room, coach, first, first.plusSeconds(3600), BigInteger.TEN);
		when(sessions.findByCourseRunIdOrderByStartAtAscIdAsc(runId)).thenReturn(List.of(earliest, session,
			new CourseSession(UUID.randomUUID(), run, 2, room, coach, last, last.plusSeconds(3600), BigInteger.TEN)));
		when(run.getPlannedStartAt()).thenReturn(first);
		service.reschedule(session.getId(), null, null, session.getStartAt().plusSeconds(3600), session.getEndAt().plusSeconds(3600));
		verify(runs, never()).saveAndFlush(any());
	}

	@Test
	void cancelledRemovedAndReplacedOriginalSessionsDoNotAnchorRunWindow() {
		Instant first = Instant.parse("2035-01-10T12:00:00Z"), last = first.plusSeconds(7 * 86400);
		CourseSession session = prepareReschedule(first, last);
		CourseSession cancelled = new CourseSession(UUID.randomUUID(), run, 3, room, coach, first.minusSeconds(86400), first.minusSeconds(82800), BigInteger.TEN);
		ReflectionTestUtils.setField(cancelled, "status", CourseSessionStatus.CANCELLED);
		CourseSession removed = new CourseSession(UUID.randomUUID(), run, 4, room, coach, last.plusSeconds(86400), last.plusSeconds(90000), BigInteger.TEN);
		ReflectionTestUtils.setField(removed, "status", CourseSessionStatus.REMOVED_BY_RUN_CHANGE);
		CourseSession original = new CourseSession(UUID.randomUUID(), run, 5, room, coach, first.minusSeconds(172800), first.minusSeconds(169200), BigInteger.TEN);
		ReflectionTestUtils.setField(original, "replacementSession", session);
		when(sessions.findByCourseRunIdOrderByStartAtAscIdAsc(runId)).thenReturn(List.of(session, cancelled, removed, original));
		service.reschedule(session.getId(), null, null, first.plusSeconds(3600), first.plusSeconds(7200));
		verify(run).changeSchedule(first.plusSeconds(3600), first.plusSeconds(7200), 12);
	}

	@Test
	void cannotMoveFirstSessionBeforeRegistrationCloses() {
		Instant first = Instant.parse("2035-01-10T12:00:00Z"), last = first.plusSeconds(7 * 86400);
		CourseSession session = prepareReschedule(first, last);
		when(run.getRegistrationClosesAt()).thenReturn(first.minusSeconds(1800));
		assertThatThrownBy(() -> service.reschedule(session.getId(), null, null, first.minusSeconds(3600), first))
			.isInstanceOf(CourseException.class);
		verifyNoInteractions(scheduleChanges);
		verify(sessions, never()).saveAndFlush(any());
	}

	@Test
	void unchangedSessionTimeDoesNotRecalculateRunWindow() {
		Instant first = Instant.parse("2035-01-10T12:00:00Z");
		CourseSession session = prepareReschedule(first, first.plusSeconds(86400));
		service.reschedule(session.getId(), null, null, session.getStartAt(), session.getEndAt());
		verify(sessions, never()).findByCourseRunIdOrderByStartAtAscIdAsc(runId);
		verify(runs, never()).saveAndFlush(any());
	}

	@Test
	void makeupCanBeCreatedAfterExistingRunEndButNormalSessionCannot() {
		Instant start = Instant.parse("2035-01-10T12:00:00Z");
		when(run.getPlannedStartAt()).thenReturn(start.minusSeconds(7 * 86400));
		when(run.getPlannedEndAt()).thenReturn(start.plusSeconds(3600));
		Instant next = start.plusSeconds(7 * 86400);
		when(sessions.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
		CourseSession makeup = service.createMakeup(runId, 1, coachId, roomId, next, next.plusSeconds(3600), BigInteger.TEN);
		assertThat(makeup.isMakeup()).isTrue();
		assertThatThrownBy(() -> service.create(runId, 2, coachId, roomId, next, next.plusSeconds(3600), BigInteger.TEN))
			.isInstanceOf(CourseException.class);
	}

	@Test
	void replacementMakeupExtendsRunEndWithoutIncreasingPurchasedUnitCount() {
		Instant first = Instant.parse("2035-01-10T12:00:00Z"), last = first.plusSeconds(7 * 86400);
		CourseSession original = prepareReschedule(first, last);
		CourseSession other = new CourseSession(UUID.randomUUID(), run, 2, room, coach, last, last.plusSeconds(3600), BigInteger.TEN);
		CourseSession makeup = new CourseSession(UUID.randomUUID(), run, 1, room, coach, last.plusSeconds(7 * 86400), last.plusSeconds(7 * 86400 + 3600), BigInteger.TEN);
		makeup.scheduleMakeup();
		original.cancelPendingMakeup();
		original.linkReplacement(makeup, first);
		when(sessions.findByCourseRunIdOrderByStartAtAscIdAsc(runId)).thenReturn(List.of(original, other, makeup));
		service.synchronizeRunWindow(run);
		verify(run).changeSchedule(last, makeup.getEndAt(), 12);
		verify(runs).saveAndFlush(run);
	}

	private CourseSession prepareReschedule(Instant editedStart, Instant otherStart) {
		CourseSession session = new CourseSession(UUID.randomUUID(), run, 1, room, coach, editedStart, editedStart.plusSeconds(3600), BigInteger.TEN);
		CourseSession other = new CourseSession(UUID.randomUUID(), run, 2, room, coach, otherStart, otherStart.plusSeconds(3600), BigInteger.TEN);
		when(run.getId()).thenReturn(runId);
		when(run.getPlannedStartAt()).thenReturn(editedStart);
		when(run.getPlannedEndAt()).thenReturn(otherStart.plusSeconds(3600));
		when(sessions.findRoomIdById(session.getId())).thenReturn(Optional.of(roomId));
		when(sessions.findCoachIdById(session.getId())).thenReturn(Optional.of(coachId));
		when(sessions.findCourseRunIdById(session.getId())).thenReturn(Optional.of(runId));
		when(sessions.findByIdForUpdate(session.getId())).thenReturn(Optional.of(session));
		when(sessions.findByCourseRunIdOrderByStartAtAscIdAsc(runId)).thenReturn(List.of(session, other));
		when(sessions.saveAndFlush(session)).thenReturn(session);
		return session;
	}

}
