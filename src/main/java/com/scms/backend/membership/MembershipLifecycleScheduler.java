package com.scms.backend.membership;

import java.time.Clock;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
class MembershipLifecycleScheduler {
	private static final Logger log = LoggerFactory.getLogger(MembershipLifecycleScheduler.class);

	private final MembershipRepository memberships;
	private final MembershipLifecycleService lifecycle;
	private final Clock clock;

	MembershipLifecycleScheduler(MembershipRepository memberships, MembershipLifecycleService lifecycle, Clock clock) {
		this.memberships = memberships;
		this.lifecycle = lifecycle;
		this.clock = clock;
	}

	@Scheduled(fixedDelayString = "${scms.membership-lifecycle-delay-ms:30000}", initialDelayString = "${scms.membership-lifecycle-initial-delay-ms:1000}")
	void reconcile() {
		var now = clock.instant();
		UUID afterMemberId = null;
		while (true) {
			var dueMembers = memberships.findDueMemberAccountIds(now, afterMemberId);
			if (dueMembers.isEmpty()) break;
			for (var memberId : dueMembers) {
				try {
					lifecycle.reconcile(memberId, now);
				} catch (RuntimeException exception) {
					log.error("Membership lifecycle reconciliation failed for member {}", memberId, exception);
				}
			}
			UUID nextAfterMemberId = dueMembers.get(dueMembers.size() - 1);
			if (nextAfterMemberId.equals(afterMemberId)) break;
			afterMemberId = nextAfterMemberId;
			if (dueMembers.size() < 100) break;
		}
		lifecycle.reconcileExpiredAssignments(now);
	}
}
