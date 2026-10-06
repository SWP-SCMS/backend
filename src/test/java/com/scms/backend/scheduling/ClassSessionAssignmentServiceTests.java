package com.scms.backend.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ClassSessionAssignmentServiceTests {

	@Mock ClassSessionRepository sessions;
	@Mock SportClassRepository classes;
	@Mock RoomRepository rooms;
	@Mock AccountRepository accounts;
	ClassSessionService service;

	private final Instant now = Instant.parse("2026-10-06T05:30:00Z");

	@BeforeEach
	void setUp() {
		service = new ClassSessionService(sessions, classes, rooms, accounts,
			Clock.fixed(now, ZoneOffset.UTC));
	}

	@Test
	void scheduledSessionCanChangeCoachAndRoomWithoutChangingSchedule() {
		UUID managerId = activeManager();
		ClassSession session = session();
		Account newCoach = coach("New Coach");
		Room newRoom = new Room(UUID.randomUUID(), "Studio B", 30, RoomStatus.ACTIVE);
		stubAssignment(session, newCoach, newRoom);
		when(sessions.existsOverlapExcluding(session.getId(), newCoach.getId(), newRoom.getId(),
			session.getStartTime(), session.getEndTime())).thenReturn(false);
		when(sessions.saveAndFlush(session)).thenReturn(session);

		Instant originalStart = session.getStartTime();
		Instant originalEnd = session.getEndTime();
		int originalCapacity = session.getCapacity();
		ClassSessionDetailResponse result = service.updateAssignment(managerId, session.getId(),
			new ClassSessionAssignmentRequest(newCoach.getId(), newRoom.getId()));

		assertThat(result.coachId()).isEqualTo(newCoach.getId());
		assertThat(result.roomId()).isEqualTo(newRoom.getId());
		assertThat(result.startTime()).isEqualTo(originalStart);
		assertThat(result.endTime()).isEqualTo(originalEnd);
		assertThat(result.capacity()).isEqualTo(originalCapacity);
	}

	@Test
	void nonScheduledSessionCannotChangeAssignment() {
		UUID managerId = activeManager();
		ClassSession session = session();
		ReflectionTestUtils.setField(session, "status", ClassSessionStatus.IN_PROGRESS);
		when(sessions.findById(session.getId())).thenReturn(Optional.of(session));

		assertThatThrownBy(() -> service.updateAssignment(managerId, session.getId(),
			new ClassSessionAssignmentRequest(UUID.randomUUID(), UUID.randomUUID())))
			.isInstanceOf(RecurringScheduleException.class)
			.extracting("code").isEqualTo("SESSION_NOT_SCHEDULED");
		verify(sessions, never()).saveAndFlush(session);
	}

	@Test
	void conflictingAssignmentIsRejectedBeforeWrite() {
		UUID managerId = activeManager();
		ClassSession session = session();
		Account newCoach = coach("New Coach");
		Room newRoom = new Room(UUID.randomUUID(), "Studio B", 30, RoomStatus.ACTIVE);
		stubAssignment(session, newCoach, newRoom);
		when(sessions.existsOverlapExcluding(session.getId(), newCoach.getId(), newRoom.getId(),
			session.getStartTime(), session.getEndTime())).thenReturn(true);

		assertThatThrownBy(() -> service.updateAssignment(managerId, session.getId(),
			new ClassSessionAssignmentRequest(newCoach.getId(), newRoom.getId())))
			.isInstanceOf(RecurringScheduleException.class)
			.extracting("code").isEqualTo("SESSION_CONFLICT");
		verify(sessions, never()).saveAndFlush(session);
	}

	private void stubAssignment(ClassSession session, Account coach, Room room) {
		when(sessions.findById(session.getId())).thenReturn(Optional.of(session));
		when(classes.findById(session.getSportClass().getId())).thenReturn(Optional.of(session.getSportClass()));
		when(accounts.findById(coach.getId())).thenReturn(Optional.of(coach));
		when(rooms.findById(room.getId())).thenReturn(Optional.of(room));
	}

	private UUID activeManager() {
		UUID id = UUID.randomUUID();
		when(accounts.existsByIdAndRoleAndStatus(id, AccountRole.MANAGER, AccountStatus.ACTIVE)).thenReturn(true);
		return id;
	}

	private ClassSession session() {
		Discipline discipline = new Discipline(UUID.randomUUID(), "Yoga", null, DisciplineStatus.ACTIVE);
		SportClass sportClass = new SportClass(UUID.randomUUID(), discipline, "Vinyasa", SportClassType.YOGA,
			null, SportClassStatus.ACTIVE);
		return new ClassSession(UUID.randomUUID(), sportClass, null, coach("Coach"),
			new Room(UUID.randomUUID(), "Studio A", 20, RoomStatus.ACTIVE), now.plusSeconds(3600),
			now.plusSeconds(7200), 15, UUID.randomUUID());
	}

	private Account coach(String name) {
		UUID id = UUID.randomUUID();
		return new Account(id, AccountRole.COACH, AccountStatus.ACTIVE, name,
			"090" + id.toString().substring(0, 8), id + "@example.test", LocalDate.of(1990, 1, 1),
			"{noop}password");
	}
}
