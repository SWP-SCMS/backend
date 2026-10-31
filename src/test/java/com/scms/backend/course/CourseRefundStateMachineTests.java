package com.scms.backend.course;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.Test;

class CourseRefundStateMachineTests {
	@Test
	void permitsOnlyForwardWorkflowAndTerminalBranches() {
		Set<String> allowed = Set.of(
			"PENDING_REVIEW->APPROVED", "PENDING_REVIEW->REJECTED",
			"APPROVED->DISBURSEMENT_RECORDED", "APPROVED->FAILED",
			"DISBURSEMENT_RECORDED->PENDING_VERIFICATION", "DISBURSEMENT_RECORDED->FAILED",
			"DISBURSEMENT_RECORDED->RETURNED", "PENDING_VERIFICATION->COMPLETED",
			"PENDING_VERIFICATION->FAILED", "PENDING_VERIFICATION->RETURNED");

		for (CourseRefundStatus from : CourseRefundStatus.values()) {
			for (CourseRefundStatus to : CourseRefundStatus.values()) {
				assertThat(CourseRefundStateMachine.canTransition(from, to))
					.as("%s -> %s", from, to)
					.isEqualTo(allowed.contains(from + "->" + to));
			}
		}
	}
}
