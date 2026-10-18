package com.scms.backend.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.audit.AuditEventRepository;
import com.scms.backend.notification.NotificationWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class ClassSessionCancellationServiceTests {

	@Mock ClassSessionRepository sessions;
	@Mock AccountRepository accounts;
	@Mock AuditEventRepository audits;
	@Mock JdbcTemplate jdbc;
	@Mock NotificationWriter notifications;
	@Mock SessionLifecycleService lifecycle;
	@Mock ExplicitBulkCancellationLockRendezvous explicitBulkLockRendezvous;
	ClassSessionCancellationService service;

	private final Instant now = Instant.parse("2026-10-06T06:00:00Z");

	@BeforeEach
	void setUp() {
		service = new ClassSessionCancellationService(sessions, accounts, audits, jdbc, notifications, lifecycle,
			Clock.fixed(now, ZoneOffset.UTC), explicitBulkLockRendezvous);
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
		verify(lifecycle).catchUp(now);
		verify(jdbc).update("""
			update bookings set status = 'CANCELLED', cancelled_by_account_id = ?,
				cancellation_source = 'SESSION_CANCELLED', cancelled_at = ?, updated_at = ?, version = version + 1
			where class_session_id = ? and status = 'BOOKED'
			""", managerId, java.sql.Timestamp.from(now), java.sql.Timestamp.from(now), session.getId());
		verify(audits).save(org.mockito.ArgumentMatchers.argThat(audit ->
			"SESSION_CANCELLED".equals(audit.getAction())
				&& managerId.equals(audit.getActorAccountId())
				&& "Coach unavailable".equals(audit.getReason())));
		verify(notifications).write("SESSION_CANCELLED:" + session.getId() + ":" + memberA, memberA,
			"SESSION_CANCELLED", "CLASS_SESSION", session.getId(), Map.of("sessionId", session.getId().toString()));
		verify(notifications).write("SESSION_CANCELLED:" + session.getId() + ":" + memberB, memberB,
			"SESSION_CANCELLED", "CLASS_SESSION", session.getId(), Map.of("sessionId", session.getId().toString()));
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

	@Test
	void previewSupportsClassAndTimeFiltersWithoutRequiringAReason() {
		UUID managerId = activeManager();
		ClassSession session = session();
		when(sessions.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of(session));

		ClassSessionCancellationPreviewResponse result = service.preview(managerId,
			new ClassSessionCancellationBatchRequest(null, session.getSportClass().getId(),
				now.plusSeconds(1800), now.plusSeconds(10800), null));

		assertThat(result.count()).isOne();
		assertThat(result.sessionIds()).containsExactly(session.getId());
	}

	@Test
	void previewRequiresAtLeastOneFilter() {
		UUID managerId = activeManager();

		assertThatThrownBy(() -> service.preview(managerId,
			new ClassSessionCancellationBatchRequest(null, null)))
			.isInstanceOf(RecurringScheduleException.class)
			.extracting("field").isEqualTo("selector");
		verify(sessions, never()).findAll(any(Specification.class), any(Sort.class));
	}

	@Test
	void bulkCancellationRetryIsIdempotentWhenAllRequestedSessionsAreAlreadyHandled() {
		UUID managerId = activeManager();
		ClassSession session = session();
		session.cancel(now.minusSeconds(60), "first cancellation");
		when(sessions.findAllByIdForUpdate(List.of(session.getId()))).thenReturn(List.of(session));

		ClassSessionCancellationBatchResponse result = service.cancelBulk(managerId,
			new ClassSessionCancellationBatchRequest(List.of(session.getId()), null, null, null, "already handled"));

		assertThat(result.cancelledCount()).isZero();
		assertThat(result.sessionIds()).isEmpty();
		verify(sessions).findAllByIdForUpdate(List.of(session.getId()));
		verify(sessions, never()).findByIdForUpdate(any());
	}

	@Test
	void bulkCancellationRejectsMixedExplicitSelectionWithEligibleAndIneligibleIds() {
		UUID managerId = activeManager();
		ClassSession eligible = session();
		UUID ineligibleId = UUID.randomUUID();
		when(sessions.findAllByIdForUpdate(List.of(ineligibleId, eligible.getId())))
			.thenReturn(List.of(eligible));

		assertThatThrownBy(() -> service.cancelBulk(managerId,
			new ClassSessionCancellationBatchRequest(List.of(ineligibleId, eligible.getId()),
				null, null, null, "stale selection")))
			.isInstanceOf(RecurringScheduleException.class)
			.hasFieldOrPropertyWithValue("status", org.springframework.http.HttpStatus.CONFLICT)
			.hasFieldOrPropertyWithValue("code", "SESSION_BULK_SELECTION_CHANGED")
			.hasFieldOrPropertyWithValue("eligibleSessionIds", List.of(eligible.getId()))
			.hasFieldOrPropertyWithValue("ineligibleSessionIds", List.of(ineligibleId));
		verify(jdbc, never()).update(org.mockito.ArgumentMatchers.anyString(),
			org.mockito.ArgumentMatchers.<Object[]>any());
		verify(audits, never()).save(any());
	}

	@Test
	void bulkCancellationCancelsEveryExplicitSessionReturnedByTheOrderedLockQuery() {
		UUID managerId = activeManager();
		ClassSession first = session(UUID.fromString("00000000-0000-0000-0000-000000000001"));
		ClassSession second = session(UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff"));
		List<UUID> requestedIds = List.of(second.getId(), first.getId());
		when(sessions.findAllByIdForUpdate(requestedIds)).thenReturn(List.of(first, second));

		ClassSessionCancellationBatchResponse result = service.cancelBulk(managerId,
			new ClassSessionCancellationBatchRequest(requestedIds, null, null, null, "weather"));

		assertThat(result.cancelledCount()).isEqualTo(2);
		assertThat(result.sessionIds()).containsExactly(first.getId(), second.getId());
		assertThat(first.getStatus()).isEqualTo(ClassSessionStatus.CANCELLED);
		assertThat(second.getStatus()).isEqualTo(ClassSessionStatus.CANCELLED);
		verify(sessions).findAllByIdForUpdate(requestedIds);
		verify(sessions, times(2)).saveAndFlush(any(ClassSession.class));
		verify(audits, times(2)).save(any());
	}

	@Test
	void explicitBulkCancellationReachesRendezvousImmediatelyBeforeOrderedLockQuery() {
		UUID managerId = activeManager();
		ClassSession session = session();
		List<UUID> requestedIds = List.of(session.getId());
		when(sessions.findAllByIdForUpdate(requestedIds)).thenReturn(List.of(session));

		service.cancelBulk(managerId,
			new ClassSessionCancellationBatchRequest(requestedIds, null, null, null, "lock boundary"));

		org.mockito.InOrder ordered = inOrder(explicitBulkLockRendezvous, sessions);
		ordered.verify(explicitBulkLockRendezvous).await();
		ordered.verify(sessions).findAllByIdForUpdate(requestedIds);
	}

	@Test
	void bulkCancellationLocksFilteredSessionsBeforeCancellingThem() {
		UUID managerId = activeManager();
		UUID classId = UUID.randomUUID();
		Instant from = now.plusSeconds(600);
		Instant to = now.plusSeconds(10800);
		ClassSession session = session();
		when(sessions.findFutureScheduledForUpdate(classId, from, to, now)).thenReturn(List.of(session));

		ClassSessionCancellationBatchResponse result = service.cancelBulk(managerId,
			new ClassSessionCancellationBatchRequest(null, classId, from, to, "maintenance"));

		assertThat(result.sessionIds()).containsExactly(session.getId());
		verify(sessions).findFutureScheduledForUpdate(classId, from, to, now);
		verify(sessions, never()).findByIdForUpdate(any());
	}

	@Test
	void bulkCancellationDoesNotTreatInProgressSessionsAsEligible() {
		UUID managerId = activeManager();
		ClassSession eligible = session(UUID.fromString("00000000-0000-0000-0000-000000000001"));
		ClassSession inProgress = session(UUID.fromString("00000000-0000-0000-0000-000000000002"));
		org.springframework.test.util.ReflectionTestUtils.setField(inProgress, "status",
			ClassSessionStatus.IN_PROGRESS);
		List<UUID> requestedIds = List.of(inProgress.getId(), eligible.getId());
		when(sessions.findAllByIdForUpdate(requestedIds)).thenReturn(List.of(eligible, inProgress));

		assertThatThrownBy(() -> service.cancelBulk(managerId,
			new ClassSessionCancellationBatchRequest(requestedIds, null, null, null, "late cancellation")))
			.isInstanceOf(RecurringScheduleException.class)
			.hasFieldOrPropertyWithValue("code", "SESSION_BULK_SELECTION_CHANGED")
			.hasFieldOrPropertyWithValue("eligibleSessionIds", List.of(eligible.getId()))
			.hasFieldOrPropertyWithValue("ineligibleSessionIds", List.of(inProgress.getId()));
		assertThat(eligible.getStatus()).isEqualTo(ClassSessionStatus.SCHEDULED);
		verify(jdbc, never()).update(org.mockito.ArgumentMatchers.anyString(),
			org.mockito.ArgumentMatchers.<Object[]>any());
	}

	private UUID activeManager() {
		UUID id = UUID.randomUUID();
		when(accounts.existsByIdAndRoleAndStatus(id, AccountRole.MANAGER, AccountStatus.ACTIVE)).thenReturn(true);
		return id;
	}

	private ClassSession session() {
		return session(UUID.randomUUID());
	}

	private ClassSession session(UUID id) {
		Discipline discipline = new Discipline(UUID.randomUUID(), "Yoga", null, DisciplineStatus.ACTIVE);
		SportClass sportClass = new SportClass(UUID.randomUUID(), discipline, "Vinyasa", SportClassType.YOGA,
			null, SportClassStatus.ACTIVE);
		Account coach = new Account(UUID.randomUUID(), AccountRole.COACH, AccountStatus.ACTIVE, "Coach",
			"0900000000", "coach@example.test", LocalDate.of(1990, 1, 1), "{noop}password");
		return new ClassSession(id, sportClass, null, coach,
			new Room(UUID.randomUUID(), "Studio A", 20, RoomStatus.ACTIVE), now.plusSeconds(3600),
			now.plusSeconds(7200), 15, UUID.randomUUID());
	}
}
