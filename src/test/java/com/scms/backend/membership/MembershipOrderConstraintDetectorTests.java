package com.scms.backend.membership;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class MembershipOrderConstraintDetectorTests {

	@Test
	void detectsOnlyThePendingMemberConstraintName() {
		DataIntegrityViolationException pending = new DataIntegrityViolationException("insert failed",
			new RuntimeException("duplicate key violates unique constraint \"uq_membership_orders_pending_member\""));
		DataIntegrityViolationException unrelated = new DataIntegrityViolationException("insert failed",
			new RuntimeException("duplicate key violates unique constraint \"membership_orders_pkey\""));

		assertThat(MembershipOrderConstraintDetector.isPendingMemberConstraint(pending)).isTrue();
		assertThat(MembershipOrderConstraintDetector.isPendingMemberConstraint(unrelated)).isFalse();
		assertThat(MembershipOrderConstraintDetector.isPendingMemberConstraint(
			new DataIntegrityViolationException("unclassified failure"))).isFalse();
	}
}
