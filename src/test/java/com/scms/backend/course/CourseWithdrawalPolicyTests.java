package com.scms.backend.course;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigInteger;

import org.junit.jupiter.api.Test;

class CourseWithdrawalPolicyTests {
	@Test
	void belowSeventyPercentCanWithdrawWithoutRefund() {
		assertThat(CourseWithdrawalPolicy.refund(6_999, 10_000, BigInteger.valueOf(999_999)))
			.isEqualTo(BigInteger.ZERO);
	}

	@Test
	void exactSeventyPercentReceivesFlooredEightyPercentOfUnusedAllocation() {
		assertThat(CourseWithdrawalPolicy.refund(7, 10, BigInteger.valueOf(1_000_001)))
			.isEqualTo(BigInteger.valueOf(800_000));
	}

	@Test
	void noRemainingUnitMeansWithdrawalWindowIsClosed() {
		assertThat(CourseWithdrawalPolicy.canWithdraw(0, 10)).isFalse();
		assertThat(CourseWithdrawalPolicy.canWithdraw(1, 10)).isTrue();
	}
}
