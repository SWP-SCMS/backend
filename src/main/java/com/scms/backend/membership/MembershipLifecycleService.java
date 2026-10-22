package com.scms.backend.membership;

import java.time.Instant;
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

	MembershipLifecycleService(AccountRepository accounts, MembershipRepository memberships,
			CoachAssignmentRepository assignments) {
		this.accounts = accounts;
		this.memberships = memberships;
		this.assignments = assignments;
	}

	@Transactional
	public void reconcile(UUID memberId, Instant now) {
		lockMember(memberId);
		Membership active = memberships.findByMemberAccountIdAndStatusForUpdate(memberId, MembershipStatus.ACTIVE)
			.orElse(null);
		if (active == null || active.getEndsAt().isAfter(now)) return;

		Instant nextStart = active.getEndsAt();
		active.expire();
		memberships.flush();
		memberships.findByMemberAccountIdAndStatusForUpdate(memberId, MembershipStatus.RESERVED)
			.ifPresent(reserved -> {
				reserved.activate(nextStart);
				if (!reserved.getEndsAt().isAfter(now)) reserved.expire();
			});
		memberships.flush();
		assignments.endActiveAssignmentIfPersonalCoachingExpired(memberId, now);
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
}
