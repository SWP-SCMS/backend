package com.scms.backend.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

@ExtendWith(MockitoExtension.class)
class ClassSessionDetailServiceTests {

	@Mock ClassSessionRepository sessions;
	@Mock SportClassRepository classes;
	@Mock RoomRepository rooms;
	@Mock AccountRepository accounts;
	ClassSessionService service;

	private final Instant now = Instant.parse("2026-10-06T05:00:00Z");

	@BeforeEach
	void setUp() {
		service = new ClassSessionService(sessions, classes, rooms, accounts,
			Clock.fixed(now, ZoneOffset.UTC));
	}

	@Test
	void scheduledSessionDetailIncludesResourcesAndAllowedActions() {
		UUID managerId = activeManager();
		ClassSession session = session();
		when(sessions.findById(session.getId())).thenReturn(Optional.of(session));

		ClassSessionDetailResponse result = service.get(managerId, session.getId());

		verify(sessions).advanceScheduledToInProgress(now);
		verify(sessions).advanceInProgressToCompleted(now);
		assertThat(result.id()).isEqualTo(session.getId());
		assertThat(result.className()).isEqualTo("Vinyasa");
		assertThat(result.disciplineName()).isEqualTo("Yoga");
		assertThat(result.coachName()).isEqualTo("Coach");
		assertThat(result.roomName()).isEqualTo("Studio");
		assertThat(result.status()).isEqualTo(ClassSessionStatus.SCHEDULED);
		assertThat(result.canUpdateAssignment()).isTrue();
		assertThat(result.canCancel()).isTrue();
	}

	@Test
	void missingSessionReturnsNotFoundAfterLifecycleRefresh() {
		UUID managerId = activeManager();
		UUID sessionId = UUID.randomUUID();
		when(sessions.findById(sessionId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.get(managerId, sessionId))
			.isInstanceOf(RecurringScheduleException.class)
			.extracting("code").isEqualTo("SESSION_NOT_FOUND");
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
		Room room = new Room(UUID.randomUUID(), "Studio", 20, RoomStatus.ACTIVE);
		UUID coachId = UUID.randomUUID();
		Account coach = new Account(coachId, AccountRole.COACH, AccountStatus.ACTIVE, "Coach",
			"090" + coachId.toString().substring(0, 8), coachId + "@example.test",
			LocalDate.of(1990, 1, 1), "{noop}password");
		return new ClassSession(UUID.randomUUID(), sportClass, null, coach, room, now.plusSeconds(3600),
			now.plusSeconds(7200), 15, UUID.randomUUID());
	}
}
