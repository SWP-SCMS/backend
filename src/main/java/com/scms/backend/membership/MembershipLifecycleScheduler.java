package com.scms.backend.membership;

import java.time.Clock;

import com.scms.backend.training.CoachAssignmentRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class MembershipLifecycleScheduler {

	private final MembershipRepository memberships;
	private final CoachAssignmentRepository assignments;
	private final Clock clock;

	MembershipLifecycleScheduler(MembershipRepository memberships, CoachAssignmentRepository assignments, Clock clock) {
		this.memberships = memberships;
		this.assignments = assignments;
		this.clock = clock;
	}

	@Scheduled(fixedDelayString = "${scms.membership-lifecycle-delay-ms:30000}", initialDelayString = "${scms.membership-lifecycle-initial-delay-ms:1000}")
	@Transactional
	void reconcile() {
		var now = clock.instant();
		memberships.expireDue(now);
		assignments.endAllActiveAssignmentsWithExpiredPlus(now);
	}
}
