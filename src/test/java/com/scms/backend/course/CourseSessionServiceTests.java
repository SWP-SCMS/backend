package com.scms.backend.course;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigInteger;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.scheduling.ClassSessionRepository;
import com.scms.backend.scheduling.CoachLeaveRepository;
import com.scms.backend.scheduling.Room;
import com.scms.backend.scheduling.RoomRepository;
import com.scms.backend.scheduling.RoomStatus;
import com.scms.backend.scheduling.SharedResourceLockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CourseSessionServiceTests {
	private final CourseRunRepository runs = mock(CourseRunRepository.class);
	private final CourseSessionRepository sessions = mock(CourseSessionRepository.class);
	private final AccountRepository accounts = mock(AccountRepository.class);
	private final RoomRepository rooms = mock(RoomRepository.class);
	private final CoachLeaveRepository leaves = mock(CoachLeaveRepository.class);
	private final ClassSessionRepository gymSessions = mock(ClassSessionRepository.class);
	private final SharedResourceLockService locks = mock(SharedResourceLockService.class);
	private CourseSessionService service;
	private CourseRun run;
	private Account coach;
	private Room room;
	private UUID runId;
	private UUID coachId;
	private UUID roomId;

	@BeforeEach
	void setUp() {
		service = new CourseSessionService(runs, sessions, accounts, rooms, leaves, gymSessions, locks);
		runId = UUID.randomUUID(); coachId = UUID.randomUUID(); roomId = UUID.randomUUID();
		run = mock(CourseRun.class); coach = mock(Account.class); room = mock(Room.class);
		when(runs.findById(runId)).thenReturn(Optional.of(run));
		when(accounts.findById(coachId)).thenReturn(Optional.of(coach));
		when(rooms.findById(roomId)).thenReturn(Optional.of(room));
		when(run.getCapacity()).thenReturn(20);
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

		assertThatThrownBy(() -> service.create(runId, 1, coachId, roomId, start, end, BigInteger.TEN))
			.isInstanceOf(CourseException.class)
			.hasMessageContaining("unavailable");
	}

	@Test
	void rejectsRoomSmallerThanRunCapacity() {
		when(room.getCapacity()).thenReturn(19);
		Instant start = Instant.parse("2035-01-01T02:00:00Z");

		assertThatThrownBy(() -> service.create(runId, 1, coachId, roomId, start,
			start.plusSeconds(3600), BigInteger.TEN))
			.isInstanceOf(CourseException.class)
			.hasMessageContaining("capacity");
	}
}
