package com.scms.backend.course;

import java.util.Map;
import java.util.Set;

final class CourseRefundStateMachine {
	private static final Map<CourseRefundStatus, Set<CourseRefundStatus>> ALLOWED = Map.of(
		CourseRefundStatus.PENDING_REVIEW, Set.of(CourseRefundStatus.APPROVED, CourseRefundStatus.REJECTED),
		CourseRefundStatus.APPROVED, Set.of(CourseRefundStatus.DISBURSEMENT_RECORDED, CourseRefundStatus.FAILED),
		CourseRefundStatus.DISBURSEMENT_RECORDED, Set.of(CourseRefundStatus.PENDING_VERIFICATION,
			CourseRefundStatus.FAILED, CourseRefundStatus.RETURNED),
		CourseRefundStatus.PENDING_VERIFICATION, Set.of(CourseRefundStatus.COMPLETED,
			CourseRefundStatus.FAILED, CourseRefundStatus.RETURNED));

	private CourseRefundStateMachine() { }

	static boolean canTransition(CourseRefundStatus from, CourseRefundStatus to) {
		return ALLOWED.getOrDefault(from, Set.of()).contains(to);
	}
}
