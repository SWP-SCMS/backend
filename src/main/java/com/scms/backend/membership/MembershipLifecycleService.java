package com.scms.backend.membership;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import com.scms.backend.account.AccountRepository;
import com.scms.backend.training.CoachAssignmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MembershipLifecycleService {

	private final AccountRepository accounts;
	private final MembershipRepository memberships;
	private final CoachAssignmentRepository assignments;
	private final RefundRequestRepository refunds;

	MembershipLifecycleService(AccountRepository accounts, MembershipRepository memberships,
			CoachAssignmentRepository assignments, RefundRequestRepository refunds) {
		this.accounts = accounts;
		this.memberships = memberships;
		this.assignments = assignments;
		this.refunds = refunds;
	}

	@Transactional
	public void reconcile(UUID memberId, Instant now) {
		lockMember(memberId);
		Membership active = memberships.findByMemberAccountIdAndStatusForUpdate(memberId, MembershipStatus.ACTIVE)
			.orElse(null);
		if (active == null) {
			activateReservedFromLatestExpired(memberId, now);
			return;
		}
		if (active.getEndsAt().isAfter(now)) return;

		Instant nextStart = active.getEndsAt();
		active.expire();
		memberships.flush();
		memberships.findByMemberAccountIdAndStatusForUpdate(memberId, MembershipStatus.RESERVED)
			.ifPresent(reserved -> {
				if (!canActivateReserved(reserved)) return;
				reserved.activate(nextStart);
				if (!reserved.getEndsAt().isAfter(now)) reserved.expire();
			});
		memberships.flush();
		assignments.endActiveAssignmentIfPersonalCoachingExpired(memberId, now);
	}

	private void activateReservedFromLatestExpired(UUID memberId, Instant now) {
		memberships.findByMemberAccountIdAndStatusForUpdate(memberId, MembershipStatus.RESERVED)
			.filter(this::canActivateReserved)
			.ifPresent(reserved -> memberships.findLatestExpiredByMemberAccountIdForUpdate(memberId, now)
				.ifPresent(expired -> {
					reserved.activate(expired.getEndsAt());
					if (!reserved.getEndsAt().isAfter(now)) reserved.expire();
					memberships.flush();
				}));
	}

	@Transactional
	public void reconcileExpiredAssignments(Instant now) {
		assignments.endAllActiveAssignmentsWithExpiredPersonalCoaching(now);
	}

	@Transactional
	public void terminateActive(UUID memberId, Instant terminatedAt) {
		lockMember(memberId);
		activeForUpdate(memberId).terminate(terminatedAt);
	}

	@Transactional
	public void terminateActiveAndActivateReserved(UUID memberId, Instant terminatedAt) {
		lockMember(memberId);
		Membership active = activeForUpdate(memberId);
		active.terminate(terminatedAt);
		memberships.flush();
		memberships.findByMemberAccountIdAndStatusForUpdate(memberId, MembershipStatus.RESERVED)
			.filter(this::canActivateReserved)
			.ifPresent(reserved -> reserved.activate(terminatedAt));
		memberships.flush();
	}

	@Transactional
	public void activateReservedAfterRefundRejected(UUID memberId, Instant rejectedAt) {
		lockMember(memberId);
		if (memberships.findByMemberAccountIdAndStatusForUpdate(memberId, MembershipStatus.ACTIVE).isPresent()) return;
		memberships.findByMemberAccountIdAndStatusForUpdate(memberId, MembershipStatus.RESERVED)
			.filter(this::canActivateReserved)
			.ifPresent(reserved -> {
				reserved.activate(rejectedAt);
				memberships.flush();
			});
	}

	@Transactional
	public void extendActive(UUID memberId, Instant newEnd) {
		lockMember(memberId);
		activeForUpdate(memberId).extendTo(newEnd);
	}

	private Membership activeForUpdate(UUID memberId) {
		return memberships.findByMemberAccountIdAndStatusForUpdate(memberId, MembershipStatus.ACTIVE)
			.orElseThrow(() -> new IllegalStateException("Member has no ACTIVE Membership"));
	}

	private void lockMember(UUID memberId) {
		accounts.findByIdForUpdate(memberId)
			.orElseThrow(() -> new IllegalStateException("Member Account was not found"));
	}

	private boolean canActivateReserved(Membership membership) {
		return !refunds.existsByMembershipIdAndTypeAndStatusIn(membership.getId(),
			RefundType.RESERVED_CANCELLATION,
			Set.of(RefundRequestStatus.REQUESTED, RefundRequestStatus.FIRST_APPROVED, RefundRequestStatus.CASH_DISBURSED));
	}
}
