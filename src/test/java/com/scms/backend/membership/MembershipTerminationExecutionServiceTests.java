package com.scms.backend.membership;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.scms.backend.account.AccountRepository;
import com.scms.backend.audit.AuditEventRepository;
import com.scms.backend.centervisit.CenterVisitRepository;
import com.scms.backend.notification.NotificationWriter;
import com.scms.backend.scheduling.BookingRepository;
import com.scms.backend.scheduling.WaitlistPromotionCoordinator;
import com.scms.backend.training.CoachAssignmentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class MembershipTerminationExecutionServiceTests {
	private static final Instant NOW = Instant.parse("2026-10-22T03:00:00Z");
	private final MembershipLifecycleService lifecycle = mock(MembershipLifecycleService.class);
	private final MembershipTerminationRequestRepository requests = mock(MembershipTerminationRequestRepository.class);
	private final MembershipRepository memberships = mock(MembershipRepository.class);
	private final AccountRepository accounts = mock(AccountRepository.class);
	private final CenterVisitRepository visits = mock(CenterVisitRepository.class);
	private final BookingRepository bookings = mock(BookingRepository.class);
	private final CoachAssignmentRepository assignments = mock(CoachAssignmentRepository.class);
	private final MembershipFreezeExecutionService freezes = mock(MembershipFreezeExecutionService.class);
	private final AuditEventRepository audits = mock(AuditEventRepository.class);
	private final NotificationWriter notifications = mock(NotificationWriter.class);
	private final WaitlistPromotionCoordinator waitlistPromotions = mock(WaitlistPromotionCoordinator.class);
	private final MembershipTerminationExecutionService service = new MembershipTerminationExecutionService(
		lifecycle, requests, memberships, accounts, visits, bookings, assignments, freezes, audits, notifications,
		waitlistPromotions);

	@Test
	void immediateExecutionTerminatesMembershipAndCleansUpFutureActivity() {
		UUID requestId = UUID.randomUUID();
		UUID memberId = UUID.randomUUID();
		UUID actorId = UUID.randomUUID();
		UUID membershipId = UUID.randomUUID();
		MembershipTerminationRequest request = mock(MembershipTerminationRequest.class);
		Membership active = mock(Membership.class);
		when(requests.findById(requestId)).thenReturn(java.util.Optional.of(request));
		when(requests.findByIdForUpdate(requestId)).thenReturn(java.util.Optional.of(request));
		when(request.getStatus()).thenReturn(MembershipTerminationRequestStatus.APPROVED);
		when(request.getMode()).thenReturn(MembershipTerminationMode.IMMEDIATE);
		when(request.getMemberAccountId()).thenReturn(memberId);
		when(request.getId()).thenReturn(requestId);
		when(request.getMembershipId()).thenReturn(membershipId);
		when(request.getReason()).thenReturn("membership termination");
		when(accounts.findByIdForUpdate(memberId)).thenReturn(java.util.Optional.of(mock(com.scms.backend.account.Account.class)));
		when(active.getId()).thenReturn(membershipId);
		when(memberships.findByMemberAccountIdAndStatusForUpdate(memberId, MembershipStatus.ACTIVE)).thenReturn(java.util.Optional.of(active));
		when(bookings.cancelFutureBookingsForMembershipTermination(memberId, actorId, NOW)).thenReturn(List.of());

		service.executeImmediate(requestId, actorId, NOW);

		verify(freezes).endActiveForTermination(memberId, NOW);
		verify(lifecycle).terminateActiveAndActivateReserved(memberId, NOW);
		verify(visits).checkoutOpenForMembershipTermination(memberId, actorId, NOW);
		verify(bookings).cancelFutureBookingsForMembershipTermination(memberId, actorId, NOW);
		verifyNoInteractions(waitlistPromotions);
		verify(assignments).endActiveAssignmentsForMembershipTermination(memberId, NOW);
		verify(request).execute();
		verify(requests).saveAndFlush(request);
		verify(audits).save(any());
		verify(notifications).write(any(), org.mockito.ArgumentMatchers.eq(memberId),
			org.mockito.ArgumentMatchers.eq("MEMBERSHIP_TERMINATION_EXECUTED"),
			org.mockito.ArgumentMatchers.eq("MEMBERSHIP_TERMINATION_REQUEST"),
			org.mockito.ArgumentMatchers.eq(requestId), any());
	}

	@Test
	void changedBookingsRegisterPromotionOnlyAfterCommit() {
		UUID requestId = UUID.randomUUID();
		UUID memberId = UUID.randomUUID();
		UUID actorId = UUID.randomUUID();
		UUID membershipId = UUID.randomUUID();
		MembershipTerminationRequest request = mock(MembershipTerminationRequest.class);
		Membership active = mock(Membership.class);
		when(requests.findById(requestId)).thenReturn(java.util.Optional.of(request));
		when(requests.findByIdForUpdate(requestId)).thenReturn(java.util.Optional.of(request));
		when(request.getStatus()).thenReturn(MembershipTerminationRequestStatus.APPROVED);
		when(request.getMode()).thenReturn(MembershipTerminationMode.IMMEDIATE);
		when(request.getMemberAccountId()).thenReturn(memberId);
		when(request.getId()).thenReturn(requestId);
		when(request.getMembershipId()).thenReturn(membershipId);
		when(request.getReason()).thenReturn("membership termination");
		when(accounts.findByIdForUpdate(memberId)).thenReturn(java.util.Optional.of(mock(com.scms.backend.account.Account.class)));
		when(active.getId()).thenReturn(membershipId);
		when(memberships.findByMemberAccountIdAndStatusForUpdate(memberId, MembershipStatus.ACTIVE))
			.thenReturn(java.util.Optional.of(active));
		UUID sessionId = UUID.randomUUID();
		when(bookings.cancelFutureBookingsForMembershipTermination(memberId, actorId, NOW))
			.thenReturn(List.of(sessionId, sessionId));

		TransactionSynchronizationManager.initSynchronization();
		try {
			service.executeImmediate(requestId, actorId, NOW);

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
	void endOfCycleIsNotExecutedWhileMembershipIsStillActive() {
		UUID requestId = UUID.randomUUID();
		MembershipTerminationRequest request = mock(MembershipTerminationRequest.class);
		Membership membership = mock(Membership.class);
		when(requests.findApprovedDueForUpdate(NOW)).thenReturn(List.of(request));
		when(request.getId()).thenReturn(requestId);
		when(request.getMode()).thenReturn(MembershipTerminationMode.END_OF_CYCLE);
		when(request.getMembershipId()).thenReturn(UUID.randomUUID());
		when(memberships.findByIdForUpdate(request.getMembershipId())).thenReturn(java.util.Optional.of(membership));
		when(membership.getStatus()).thenReturn(MembershipStatus.ACTIVE);

		service.reconcileDue(NOW);

		verifyNoInteractions(lifecycle, visits, bookings, assignments, audits, notifications);
	}

	@Test
	void endOfCycleIsExecutedAfterMembershipExpires() {
		UUID requestId = UUID.randomUUID();
		UUID memberId = UUID.randomUUID();
		UUID membershipId = UUID.randomUUID();
		UUID managerId = UUID.randomUUID();
		MembershipTerminationRequest request = mock(MembershipTerminationRequest.class);
		Membership membership = mock(Membership.class);
		when(requests.findApprovedDueForUpdate(NOW)).thenReturn(List.of(request));
		when(request.getId()).thenReturn(requestId);
		when(request.getMode()).thenReturn(MembershipTerminationMode.END_OF_CYCLE);
		when(request.getStatus()).thenReturn(MembershipTerminationRequestStatus.EXECUTED);
		when(request.getMembershipId()).thenReturn(membershipId);
		when(request.getMemberAccountId()).thenReturn(memberId);
		when(request.getApproverAccountId()).thenReturn(managerId);
		when(request.getReason()).thenReturn("end of cycle");
		when(memberships.findByIdForUpdate(membershipId)).thenReturn(java.util.Optional.of(membership));
		when(membership.getStatus()).thenReturn(MembershipStatus.EXPIRED);

		service.reconcileDue(NOW);

		verify(request).execute();
		verify(requests).saveAndFlush(request);
		verify(audits).save(any());
		verify(notifications).write(any(), org.mockito.ArgumentMatchers.eq(memberId),
			org.mockito.ArgumentMatchers.eq("MEMBERSHIP_TERMINATION_EXECUTED"), any(),
			org.mockito.ArgumentMatchers.eq(requestId), any());
		verifyNoInteractions(lifecycle, visits, bookings, assignments);
	}
}
