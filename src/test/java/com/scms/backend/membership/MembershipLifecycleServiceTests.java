package com.scms.backend.membership;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigInteger;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.Set;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.training.CoachAssignmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class MembershipLifecycleServiceTests {

	private static final Instant NOW = Instant.parse("2026-10-21T03:00:00Z");
	private final AccountRepository accounts = mock(AccountRepository.class);
	private final MembershipRepository memberships = mock(MembershipRepository.class);
	private final CoachAssignmentRepository assignments = mock(CoachAssignmentRepository.class);
	private final RefundRequestRepository refunds = mock(RefundRequestRepository.class);
	private final MembershipLifecycleService service = new MembershipLifecycleService(accounts, memberships, assignments, refunds);
	private final UUID memberId = UUID.randomUUID();

	@BeforeEach
	void locksMember() {
		when(accounts.findByIdForUpdate(memberId)).thenReturn(Optional.of(mock(Account.class)));
	}

	@Test
	void expiresDueActiveAndActivatesReservedFromThePreviousEnd() {
		Instant previousEnd = NOW.minusSeconds(3600);
		Membership active = membership(MembershipStatus.ACTIVE, NOW.minusSeconds(86400), previousEnd, 30);
		Membership reserved = membership(MembershipStatus.RESERVED, null, null, 45);
		when(memberships.findByMemberAccountIdAndStatusForUpdate(memberId, MembershipStatus.ACTIVE))
			.thenReturn(Optional.of(active));
		when(memberships.findByMemberAccountIdAndStatusForUpdate(memberId, MembershipStatus.RESERVED))
			.thenReturn(Optional.of(reserved));

		service.reconcile(memberId, NOW);

		assertThat(active.getStatus()).isEqualTo(MembershipStatus.EXPIRED);
		assertThat(reserved.getStatus()).isEqualTo(MembershipStatus.ACTIVE);
		assertThat(reserved.getStartsAt()).isEqualTo(previousEnd);
		assertThat(reserved.getEndsAt()).isEqualTo(previousEnd.plusSeconds(45L * 86400));
		InOrder order = inOrder(accounts, memberships, assignments);
		order.verify(accounts).findByIdForUpdate(memberId);
		order.verify(memberships).findByMemberAccountIdAndStatusForUpdate(memberId, MembershipStatus.ACTIVE);
		order.verify(memberships).flush();
		order.verify(memberships).findByMemberAccountIdAndStatusForUpdate(memberId, MembershipStatus.RESERVED);
		order.verify(memberships).flush();
		order.verify(assignments).endActiveAssignmentIfPersonalCoachingExpired(memberId, NOW);
	}

	@Test
	void retryLeavesCurrentActiveMembershipUntouched() {
		Membership active = membership(MembershipStatus.ACTIVE, NOW.minusSeconds(60), NOW.plusSeconds(86400), 30);
		when(memberships.findByMemberAccountIdAndStatusForUpdate(memberId, MembershipStatus.ACTIVE))
			.thenReturn(Optional.of(active));

		service.reconcile(memberId, NOW);

		assertThat(active.getStatus()).isEqualTo(MembershipStatus.ACTIVE);
		verify(memberships, never()).findByMemberAccountIdAndStatusForUpdate(memberId, MembershipStatus.RESERVED);
		verify(assignments, never()).endActiveAssignmentIfPersonalCoachingExpired(memberId, NOW);
	}

	@Test
	void activatesReservedMembershipFromLatestExpiredMembershipWhenActiveIsAlreadyGone() {
		Instant previousEnd = NOW.minusSeconds(3600);
		Membership reserved = membership(MembershipStatus.RESERVED, null, null, 45);
		Membership expired = membership(MembershipStatus.EXPIRED, NOW.minusSeconds(86400), previousEnd, 30);
		when(memberships.findByMemberAccountIdAndStatusForUpdate(memberId, MembershipStatus.ACTIVE))
			.thenReturn(Optional.empty());
		when(memberships.findByMemberAccountIdAndStatusForUpdate(memberId, MembershipStatus.RESERVED))
			.thenReturn(Optional.of(reserved));
		when(memberships.findLatestExpiredByMemberAccountIdForUpdate(memberId, NOW))
			.thenReturn(Optional.of(expired));

		service.reconcile(memberId, NOW);

		assertThat(reserved.getStatus()).isEqualTo(MembershipStatus.ACTIVE);
		assertThat(reserved.getStartsAt()).isEqualTo(previousEnd);
		assertThat(reserved.getEndsAt()).isEqualTo(previousEnd.plusSeconds(45L * 86400));
	}

	@Test
	void expiresReservationImmediatelyWhenReconciliationRunsAfterItsFullDuration() {
		Instant previousEnd = NOW.minusSeconds(60L * 86400);
		Membership active = membership(MembershipStatus.ACTIVE, previousEnd.minusSeconds(86400), previousEnd, 30);
		Membership reserved = membership(MembershipStatus.RESERVED, null, null, 30);
		when(memberships.findByMemberAccountIdAndStatusForUpdate(memberId, MembershipStatus.ACTIVE))
			.thenReturn(Optional.of(active));
		when(memberships.findByMemberAccountIdAndStatusForUpdate(memberId, MembershipStatus.RESERVED))
			.thenReturn(Optional.of(reserved));

		service.reconcile(memberId, NOW);

		assertThat(active.getStatus()).isEqualTo(MembershipStatus.EXPIRED);
		assertThat(reserved.getStatus()).isEqualTo(MembershipStatus.EXPIRED);
	}

	@Test
	void extendLocksMemberBeforeActiveMembership() {
		Membership active = membership(MembershipStatus.ACTIVE, NOW.minusSeconds(86400), NOW.plusSeconds(86400), 30);
		when(memberships.findByMemberAccountIdAndStatusForUpdate(memberId, MembershipStatus.ACTIVE))
			.thenReturn(Optional.of(active));

		service.extendActive(memberId, NOW.plusSeconds(172800));

		assertThat(active.getEndsAt()).isEqualTo(NOW.plusSeconds(172800));
		InOrder order = inOrder(accounts, memberships);
		order.verify(accounts).findByIdForUpdate(memberId);
		order.verify(memberships).findByMemberAccountIdAndStatusForUpdate(memberId, MembershipStatus.ACTIVE);
	}

	@Test
	void terminateLocksMemberBeforeActiveMembership() {
		Membership active = membership(MembershipStatus.ACTIVE, NOW.minusSeconds(86400), NOW.plusSeconds(86400), 30);
		when(memberships.findByMemberAccountIdAndStatusForUpdate(memberId, MembershipStatus.ACTIVE))
			.thenReturn(Optional.of(active));

		service.terminateActive(memberId, NOW);

		assertThat(active.getStatus()).isEqualTo(MembershipStatus.TERMINATED);
		assertThat(active.getEndsAt()).isEqualTo(NOW);
		InOrder order = inOrder(accounts, memberships);
		order.verify(accounts).findByIdForUpdate(memberId);
		order.verify(memberships).findByMemberAccountIdAndStatusForUpdate(memberId, MembershipStatus.ACTIVE);
	}

	@Test
	void immediateTerminationActivatesReservedMembershipFromTerminationTime() {
		Membership active = membership(MembershipStatus.ACTIVE, NOW.minusSeconds(86400), NOW.plusSeconds(86400), 30);
		Membership reserved = membership(MembershipStatus.RESERVED, null, null, 45);
		when(memberships.findByMemberAccountIdAndStatusForUpdate(memberId, MembershipStatus.ACTIVE))
			.thenReturn(Optional.of(active));
		when(memberships.findByMemberAccountIdAndStatusForUpdate(memberId, MembershipStatus.RESERVED))
			.thenReturn(Optional.of(reserved));

		service.terminateActiveAndActivateReserved(memberId, NOW);

		assertThat(active.getStatus()).isEqualTo(MembershipStatus.TERMINATED);
		assertThat(reserved.getStatus()).isEqualTo(MembershipStatus.ACTIVE);
		assertThat(reserved.getStartsAt()).isEqualTo(NOW);
		assertThat(reserved.getEndsAt()).isEqualTo(NOW.plusSeconds(45L * 86400));
	}

	@Test
	void dueReservedMembershipStaysReservedWhileRefundHasBeenApprovedForDisbursement() {
		Membership reserved = membership(MembershipStatus.RESERVED, null, null, 45);
		when(memberships.findByMemberAccountIdAndStatusForUpdate(memberId, MembershipStatus.ACTIVE)).thenReturn(Optional.empty());
		when(memberships.findByMemberAccountIdAndStatusForUpdate(memberId, MembershipStatus.RESERVED)).thenReturn(Optional.of(reserved));
		when(refunds.existsByMembershipIdAndTypeAndStatusIn(reserved.getId(), RefundType.RESERVED_CANCELLATION,
			Set.of(RefundRequestStatus.REQUESTED, RefundRequestStatus.FIRST_APPROVED, RefundRequestStatus.CASH_DISBURSED))).thenReturn(true);

		service.reconcile(memberId, NOW);

		assertThat(reserved.getStatus()).isEqualTo(MembershipStatus.RESERVED);
		verify(memberships, never()).findLatestExpiredByMemberAccountIdForUpdate(memberId, NOW);
	}

	@Test
	void immediateTerminationDoesNotActivateReservedMembershipWithOpenRefundRequest() {
		Membership active = membership(MembershipStatus.ACTIVE, NOW.minusSeconds(86400), NOW.plusSeconds(86400), 30);
		Membership reserved = membership(MembershipStatus.RESERVED, null, null, 45);
		when(memberships.findByMemberAccountIdAndStatusForUpdate(memberId, MembershipStatus.ACTIVE)).thenReturn(Optional.of(active));
		when(memberships.findByMemberAccountIdAndStatusForUpdate(memberId, MembershipStatus.RESERVED)).thenReturn(Optional.of(reserved));
		when(refunds.existsByMembershipIdAndTypeAndStatusIn(reserved.getId(), RefundType.RESERVED_CANCELLATION,
			Set.of(RefundRequestStatus.REQUESTED, RefundRequestStatus.FIRST_APPROVED, RefundRequestStatus.CASH_DISBURSED))).thenReturn(true);

		service.terminateActiveAndActivateReserved(memberId, NOW);

		assertThat(active.getStatus()).isEqualTo(MembershipStatus.TERMINATED);
		assertThat(reserved.getStatus()).isEqualTo(MembershipStatus.RESERVED);
	}

	@Test
	void rejectedReservedRefundActivatesReservationFromRejectionTimeWhenPreviousMembershipWasTerminated() {
		Membership reserved = membership(MembershipStatus.RESERVED, null, null, 45);
		when(memberships.findByMemberAccountIdAndStatusForUpdate(memberId, MembershipStatus.ACTIVE)).thenReturn(Optional.empty());
		when(memberships.findByMemberAccountIdAndStatusForUpdate(memberId, MembershipStatus.RESERVED)).thenReturn(Optional.of(reserved));

		service.activateReservedAfterRefundRejected(memberId, NOW);

		assertThat(reserved.getStatus()).isEqualTo(MembershipStatus.ACTIVE);
		assertThat(reserved.getStartsAt()).isEqualTo(NOW);
		assertThat(reserved.getEndsAt()).isEqualTo(NOW.plusSeconds(45L * 86400));
	}

	private Membership membership(MembershipStatus status, Instant startsAt, Instant endsAt, int durationDays) {
		return new Membership(UUID.randomUUID(), memberId, UUID.randomUUID(), UUID.randomUUID(), "PLUS", "Plus",
			BigInteger.valueOf(1_000_000), "VND", durationDays, status, startsAt, endsAt);
	}
}
