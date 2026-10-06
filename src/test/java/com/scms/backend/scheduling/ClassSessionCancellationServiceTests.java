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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.audit.AuditEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class ClassSessionCancellationServiceTests {

	@Mock ClassSessionRepository sessions;
	@Mock AccountRepository accounts;
	@Mock AuditEventRepository audits;
	@Mock JdbcTemplate jdbc;
	@Mock ApplicationEventPublisher events;
	ClassSessionCancellationService service;

	private final Instant now = Instant.parse("2026-10-06T06:00:00Z");

	@BeforeEach
	void setUp() {
		service = new ClassSessionCancellationService(sessions, accounts, audits, jdbc, events,
			Clock.fixed(now, ZoneOffset.UTC));
	}

	@Test
	void cancelScheduledSessionCancelsBookingsAndPublishesAffectedMembers() {
		UUID managerId = activeManager();
		ClassSession session = session();
		UUID memberA = UUID.randomUUID();
		UUID memberB = UUID.randomUUID();
		when(sessions.findByIdForUpdate(session.getId())).thenReturn(Optional.of(session));
		when(jdbc.queryForList("""
			select member_account_id from bookings
			where class_session_id = ? and status = 'BOOKED'
			for update
			""", UUID.class, session.getId())).thenReturn(List.of(memberA, memberB));

		ClassSessionDetailResponse result = service.cancel(managerId, session.getId(),
			new ClassSessionCancellationRequest("Coach unavailable"));

		assertThat(result.status()).isEqualTo(ClassSessionStatus.CANCELLED);
		assertThat(result.cancelledAt()).isEqualTo(now);
		assertThat(result.cancellationReason()).isEqualTo("Coach unavailable");
		verify(jdbc).update("""
			update bookings set status = 'CANCELLED', cancelled_by_account_id = ?,
				cancellation_source = 'SESSION_CANCELLED', cancelled_at = ?, updated_at = ?, version = version + 1
			where class_session_id = ? and status = 'BOOKED'
			""", managerId, java.sql.Timestamp.from(now), java.sql.Timestamp.from(now), session.getId());
		verify(audits).save(org.mockito.ArgumentMatchers.argThat(audit ->
			"SESSION_CANCELLED".equals(audit.getAction())
				&& managerId.equals(audit.getActorAccountId())
				&& "Coach unavailable".equals(audit.getReason())));
		verify(events).publishEvent(new ClassSessionCancelledEvent(session.getId(), List.of(memberA, memberB)));
	}

	@Test
	void blankReasonIsRejectedBeforeReadingSession() {
		UUID managerId = activeManager();

		assertThatThrownBy(() -> service.cancel(managerId, UUID.randomUUID(),
			new ClassSessionCancellationRequest("  ")))
			.isInstanceOf(RecurringScheduleException.class)
			.extracting("code").isEqualTo("VALIDATION_ERROR");
		verify(sessions, never()).findByIdForUpdate(org.mockito.ArgumentMatchers.any());
	}

	@Test
	void nonScheduledSessionCannotBeCancelled() {
		UUID managerId = activeManager();
		ClassSession session = session();
		session.cancel(now, "first cancellation");
		when(sessions.findByIdForUpdate(session.getId())).thenReturn(Optional.of(session));

		assertThatThrownBy(() -> service.cancel(managerId, session.getId(),
			new ClassSessionCancellationRequest("again")))
			.isInstanceOf(RecurringScheduleException.class)
			.extracting("code").isEqualTo("SESSION_NOT_SCHEDULED");
		verify(jdbc, never()).update(org.mockito.ArgumentMatchers.anyString(),
			org.mockito.ArgumentMatchers.<Object[]>any());
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
		Account coach = new Account(UUID.randomUUID(), AccountRole.COACH, AccountStatus.ACTIVE, "Coach",
			"0900000000", "coach@example.test", LocalDate.of(1990, 1, 1), "{noop}password");
		return new ClassSession(UUID.randomUUID(), sportClass, null, coach,
			new Room(UUID.randomUUID(), "Studio A", 20, RoomStatus.ACTIVE), now.plusSeconds(3600),
			now.plusSeconds(7200), 15, UUID.randomUUID());
	}
}
