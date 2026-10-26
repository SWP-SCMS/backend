package com.scms.backend.membership;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigInteger;
import java.sql.Date;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.scms.backend.account.AccountRepository;
import com.scms.backend.audit.AuditEventRepository;
import com.scms.backend.centervisit.CenterVisitRepository;
import com.scms.backend.notification.NotificationWriter;
import com.scms.backend.scheduling.BookingRepository;
import com.scms.backend.scheduling.WaitlistPromotionCoordinator;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class MembershipFreezeExecutionServiceTests {
	private final MembershipFreezeRequestRepository requests = mock(MembershipFreezeRequestRepository.class);
	private final MembershipRepository memberships = mock(MembershipRepository.class);
	private final AccountRepository accounts = mock(AccountRepository.class);
	private final BookingRepository bookings = mock(BookingRepository.class);
	private final CenterVisitRepository visits = mock(CenterVisitRepository.class);
	private final AuditEventRepository audits = mock(AuditEventRepository.class);
	private final NotificationWriter notifications = mock(NotificationWriter.class);
	private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
	private final PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
	private final TransactionStatus transactionStatus = mock(TransactionStatus.class);
	private final WaitlistPromotionCoordinator waitlistPromotions = mock(WaitlistPromotionCoordinator.class);
	private final MembershipFreezeExecutionService service = new MembershipFreezeExecutionService(requests, memberships,
		accounts, bookings, visits, audits, notifications, jdbc,
		Clock.fixed(Instant.parse("2026-11-02T00:00:00Z"), ZoneOffset.UTC), transactionManager, waitlistPromotions);

	MembershipFreezeExecutionServiceTests() {
		when(transactionManager.getTransaction(org.mockito.ArgumentMatchers.any(TransactionDefinition.class)))
			.thenReturn(transactionStatus);
	}

	@Test
	void dueFreezeForExpiredMembershipBecomesObsoleteWithoutActivation() {
		UUID requestId = UUID.randomUUID();
		UUID memberId = UUID.randomUUID();
		MembershipFreezeRequest request = mock(MembershipFreezeRequest.class);
		Membership membership = mock(Membership.class);
		when(requests.findById(requestId)).thenReturn(Optional.of(request));
		when(request.getStatus()).thenReturn(MembershipFreezeRequestStatus.SCHEDULED);
		when(request.getStartDate()).thenReturn(LocalDate.of(2026, 11, 1));
		when(request.getMemberAccountId()).thenReturn(memberId);
		when(request.getMembershipId()).thenReturn(UUID.randomUUID());
		when(accounts.findByIdForUpdate(memberId)).thenReturn(Optional.of(mock(com.scms.backend.account.Account.class)));
		when(memberships.findByIdForUpdate(request.getMembershipId())).thenReturn(Optional.of(membership));
		when(membership.getStatus()).thenReturn(MembershipStatus.EXPIRED);
		when(requests.findByIdForUpdate(requestId)).thenReturn(Optional.of(request));

		service.activate(requestId, LocalDate.of(2026, 11, 2), Instant.parse("2026-11-02T00:00:00Z"));

		verify(request).obsolete();
		verify(requests).saveAndFlush(request);
		verify(jdbc).update(org.mockito.ArgumentMatchers.contains("status='OBSOLETE'"), org.mockito.ArgumentMatchers.eq(requestId));
		verify(bookings, org.mockito.Mockito.never()).cancelFutureBookingsForMembershipFreeze(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
	}

	@Test
	void completionCreditsActualHalfOpenDaysAndExtendsMembershipExactlyOnce() {
		UUID requestId = UUID.randomUUID();
		UUID membershipId = UUID.randomUUID();
		UUID memberId = UUID.randomUUID();
		MembershipFreezeRequest request = new MembershipFreezeRequest(requestId, membershipId, memberId, memberId,
			LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 5), "travel");
		request.approve(UUID.randomUUID(), BigInteger.valueOf(100), Instant.parse("2026-10-22T00:00:00Z"));
		request.schedule();
		request.activate();
		Instant originalEnd = Instant.parse("2026-12-01T00:00:00Z");
		Membership membership = new Membership(membershipId, memberId, UUID.randomUUID(), UUID.randomUUID(),
			"PLUS", "Offer", BigInteger.valueOf(100_000), "VND", 30, MembershipStatus.ACTIVE,
			Instant.parse("2026-11-01T00:00:00Z"), originalEnd);
		when(requests.findById(requestId)).thenReturn(Optional.of(request));
		when(accounts.findByIdForUpdate(memberId)).thenReturn(Optional.of(mock(com.scms.backend.account.Account.class)));
		when(memberships.findByIdForUpdate(membershipId)).thenReturn(Optional.of(membership));
		when(requests.findByIdForUpdate(requestId)).thenReturn(Optional.of(request));
		when(jdbc.queryForObject(contains("actual_start_date"), eq(LocalDate.class), eq(requestId)))
			.thenReturn(LocalDate.of(2026, 11, 2));
		when(jdbc.update(contains("status='COMPLETED'"), eq(Date.valueOf("2026-11-05")), eq(3), eq(requestId)))
			.thenReturn(1);

		service.complete(requestId, LocalDate.of(2026, 11, 5));
		service.complete(requestId, LocalDate.of(2026, 11, 5));

		assertThat(membership.getEndsAt()).isEqualTo(originalEnd.plusSeconds(3 * 86_400L));
		assertThat(request.getStatus()).isEqualTo(MembershipFreezeRequestStatus.COMPLETED);
		verify(jdbc, times(1)).update(contains("status='COMPLETED'"), eq(Date.valueOf("2026-11-05")), eq(3), eq(requestId));
		verify(requests, times(1)).saveAndFlush(request);
		verify(audits).save(org.mockito.ArgumentMatchers.any());
		verify(notifications).write(eq("MEMBERSHIP_FREEZE_COMPLETED:" + requestId), eq(memberId),
			eq("MEMBERSHIP_FREEZE_COMPLETED"), eq("MEMBERSHIP_FREEZE_REQUEST"), eq(requestId),
			eq(Map.of("creditedDays", 3)));
	}

	@Test
	void activatedFreezeRegistersChangedSessionsOnlyAfterCommit() {
		UUID requestId = UUID.randomUUID();
		UUID membershipId = UUID.randomUUID();
		UUID memberId = UUID.randomUUID();
		UUID sessionId = UUID.randomUUID();
		MembershipFreezeRequest request = new MembershipFreezeRequest(requestId, membershipId, memberId, memberId,
			LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 5), "travel");
		request.approve(UUID.randomUUID(), BigInteger.valueOf(100), Instant.parse("2026-10-22T00:00:00Z"));
		request.schedule();
		Membership membership = new Membership(membershipId, memberId, UUID.randomUUID(), UUID.randomUUID(),
			"PLUS", "Offer", BigInteger.valueOf(100_000), "VND", 30, MembershipStatus.ACTIVE,
			Instant.parse("2026-11-01T00:00:00Z"), Instant.parse("2026-12-01T00:00:00Z"));
		when(requests.findById(requestId)).thenReturn(Optional.of(request));
		when(accounts.findByIdForUpdate(memberId)).thenReturn(Optional.of(mock(com.scms.backend.account.Account.class)));
		when(memberships.findByIdForUpdate(membershipId)).thenReturn(Optional.of(membership));
		when(requests.findByIdForUpdate(requestId)).thenReturn(Optional.of(request));
		when(jdbc.update(contains("status='ACTIVE'"), eq(Date.valueOf("2026-11-01")), eq(requestId))).thenReturn(1);
		when(bookings.cancelFutureBookingsForMembershipFreeze(eq(memberId), eq(null),
				eq(Instant.parse("2026-10-31T17:00:00Z")), eq(Instant.parse("2026-11-04T17:00:00Z")),
				eq(Instant.parse("2026-11-02T00:00:00Z")))).thenReturn(List.of(sessionId));

		TransactionSynchronizationManager.initSynchronization();
		try {
			service.activate(requestId, LocalDate.of(2026, 11, 2), Instant.parse("2026-11-02T00:00:00Z"));

			var synchronizations = TransactionSynchronizationManager.getSynchronizations();
			assertThat(synchronizations).hasSize(1);
			verifyNoInteractions(waitlistPromotions);
			synchronizations.getFirst().afterCommit();
			verify(waitlistPromotions).promote(sessionId);
		}
		finally {
			TransactionSynchronizationManager.clearSynchronization();
		}
	}

	@Test
	void lateSchedulerCompletesAWholeScheduledFreezeWithoutLeavingItActive() {
		UUID requestId = UUID.randomUUID();
		UUID membershipId = UUID.randomUUID();
		UUID memberId = UUID.randomUUID();
		MembershipFreezeRequest request = new MembershipFreezeRequest(requestId, membershipId, memberId, memberId,
			LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 3), "travel");
		request.approve(UUID.randomUUID(), BigInteger.valueOf(100), Instant.parse("2026-10-22T00:00:00Z"));
		request.schedule();
		Instant originalEnd = Instant.parse("2026-12-01T00:00:00Z");
		Membership membership = new Membership(membershipId, memberId, UUID.randomUUID(), UUID.randomUUID(), "PLUS", "Offer",
			BigInteger.valueOf(100_000), "VND", 30, MembershipStatus.ACTIVE, Instant.parse("2026-11-01T00:00:00Z"), originalEnd);
		when(requests.findById(requestId)).thenReturn(Optional.of(request));
		when(accounts.findByIdForUpdate(memberId)).thenReturn(Optional.of(mock(com.scms.backend.account.Account.class)));
		when(memberships.findByIdForUpdate(membershipId)).thenReturn(Optional.of(membership));
		when(requests.findByIdForUpdate(requestId)).thenReturn(Optional.of(request));
		when(jdbc.update(contains("status='COMPLETED'"), eq(Date.valueOf("2026-11-01")), eq(Date.valueOf("2026-11-03")), eq(2), eq(requestId))).thenReturn(1);

		service.activate(requestId, LocalDate.of(2026, 11, 5), Instant.parse("2026-11-04T17:00:00Z"));

		assertThat(request.getStatus()).isEqualTo(MembershipFreezeRequestStatus.COMPLETED);
		assertThat(membership.getEndsAt()).isEqualTo(originalEnd.plusSeconds(2 * 86_400L));
		verify(jdbc).update(contains("status='COMPLETED'"), eq(Date.valueOf("2026-11-01")), eq(Date.valueOf("2026-11-03")), eq(2), eq(requestId));
	}

	@Test
	void reconciliationCompletesThenActivatesEachDueRequestInItsOwnTransaction() {
		UUID activeId = UUID.randomUUID();
		UUID scheduledId = UUID.randomUUID();
		LocalDate businessDate = LocalDate.of(2026, 11, 2);
		Instant now = Instant.parse("2026-11-01T17:00:00Z");
		when(requests.findDueActiveIds(businessDate)).thenReturn(List.of(activeId));
		when(requests.findDueScheduledIds(businessDate)).thenReturn(List.of(scheduledId));
		when(requests.findById(activeId)).thenReturn(Optional.empty());
		when(requests.findById(scheduledId)).thenReturn(Optional.empty());

		service.reconcileDue(now);

		org.mockito.InOrder order = org.mockito.Mockito.inOrder(requests);
		order.verify(requests).findDueActiveIds(businessDate);
		order.verify(requests).findById(activeId);
		order.verify(requests).findDueScheduledIds(businessDate);
		order.verify(requests).findById(scheduledId);
		verify(transactionManager, times(2)).getTransaction(org.mockito.ArgumentMatchers.any(TransactionDefinition.class));
		verify(transactionManager, times(2)).commit(transactionStatus);
	}

	@Test
	void unpaidApprovedFreezeBecomesObsoleteWhenItsStartDateArrives() {
		UUID requestId = UUID.randomUUID();
		LocalDate businessDate = LocalDate.of(2026, 11, 2);
		MembershipFreezeRequest request = mock(MembershipFreezeRequest.class);
		when(requests.findDueUnpaidIds(businessDate)).thenReturn(List.of(requestId));
		when(requests.findDueActiveIds(businessDate)).thenReturn(List.of());
		when(requests.findDueScheduledIds(businessDate)).thenReturn(List.of());
		when(requests.findByIdForUpdate(requestId)).thenReturn(Optional.of(request));
		when(request.getStatus()).thenReturn(MembershipFreezeRequestStatus.APPROVED_PENDING_PAYMENT);

		service.reconcileDue(Instant.parse("2026-11-01T17:00:00Z"));

		verify(request).obsolete();
		verify(requests).saveAndFlush(request);
	}

	@Test
	void schedulerReconcilesFreezeBeforeNormalMembershipExpiry() {
		Instant now = Instant.parse("2026-11-01T17:00:00Z");
		UUID memberId = UUID.randomUUID();
		MembershipRepository schedulerMemberships = mock(MembershipRepository.class);
		MembershipLifecycleService lifecycle = mock(MembershipLifecycleService.class);
		MembershipFreezeExecutionService freeze = mock(MembershipFreezeExecutionService.class);
		when(schedulerMemberships.findDueMemberAccountIds(now, null)).thenReturn(List.of(memberId));

		new MembershipLifecycleScheduler(schedulerMemberships, lifecycle, null, freeze,
			Clock.fixed(now, ZoneOffset.UTC)).reconcile();

		org.mockito.InOrder order = org.mockito.Mockito.inOrder(freeze, lifecycle);
		order.verify(freeze).reconcileDue(now);
		order.verify(lifecycle).reconcile(memberId, now);
	}
}
