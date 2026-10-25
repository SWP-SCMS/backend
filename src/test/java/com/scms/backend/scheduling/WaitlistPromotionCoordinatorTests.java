package com.scms.backend.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;

class WaitlistPromotionCoordinatorTests {
	private static final Instant NOW = Instant.parse("2026-10-24T03:00:00Z");
	private final ClassSessionRepository sessions = mock(ClassSessionRepository.class);
	private final BookingRepository bookings = mock(BookingRepository.class);
	private final WaitlistPromotionService promotions = mock(WaitlistPromotionService.class);
	private final PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
	private final TransactionStatus transactionStatus = mock(TransactionStatus.class);
	private final WaitlistPromotionCoordinator coordinator = new WaitlistPromotionCoordinator(sessions, bookings,
		promotions, Clock.fixed(NOW, ZoneOffset.UTC), transactionManager);

	@BeforeEach
	void setUp() {
		when(transactionManager.getTransaction(any(TransactionDefinition.class))).thenReturn(transactionStatus);
	}

	@Test
	void eachSessionUsesRequiresNewAndLocksBeforePromotion() {
		ClassSession session = session(NOW.plusSeconds(3600));
		when(sessions.findByIdForUpdate(session.getId())).thenReturn(Optional.of(session));

		coordinator.promote(session.getId());

		ArgumentCaptor<TransactionDefinition> definition = ArgumentCaptor.forClass(TransactionDefinition.class);
		verify(transactionManager).getTransaction(definition.capture());
		assertThat(definition.getValue().getPropagationBehavior())
			.isEqualTo(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		verify(promotions).promoteAvailable(session);
		verify(transactionManager).commit(transactionStatus);
	}

	@Test
	void startedAndCancelledSessionsAreIgnored() {
		ClassSession started = session(NOW);
		ClassSession cancelled = session(NOW.plusSeconds(3600));
		cancelled.cancel(NOW, "cancelled");
		when(sessions.findByIdForUpdate(started.getId())).thenReturn(Optional.of(started));
		when(sessions.findByIdForUpdate(cancelled.getId())).thenReturn(Optional.of(cancelled));

		coordinator.promote(started.getId());
		coordinator.promote(cancelled.getId());

		verify(promotions, never()).promoteAvailable(any());
		verify(transactionManager, times(2)).commit(transactionStatus);
	}

	private ClassSession session(Instant start) {
		UUID id = UUID.randomUUID();
		Account coach = new Account(UUID.randomUUID(), AccountRole.COACH, AccountStatus.ACTIVE, "Coach",
			"090" + id.toString().substring(0, 8), id + "@test.local", LocalDate.of(1990, 1, 1), "{noop}x");
		Discipline discipline = new Discipline(UUID.randomUUID(), "Group", null, DisciplineStatus.ACTIVE);
		SportClass sportClass = new SportClass(UUID.randomUUID(), discipline, "Group", SportClassType.GROUP,
			null, SportClassStatus.ACTIVE);
		return new ClassSession(UUID.randomUUID(), sportClass, null, coach,
			new Room(UUID.randomUUID(), "Room", 20, RoomStatus.ACTIVE), start, start.plusSeconds(3600), 1,
			UUID.randomUUID());
	}
}
