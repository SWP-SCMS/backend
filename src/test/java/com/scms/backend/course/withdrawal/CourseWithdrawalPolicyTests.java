package com.scms.backend.course.withdrawal;

import static org.assertj.core.api.Assertions.assertThat;
import com.scms.backend.course.CourseWithdrawalCause;
import java.math.BigInteger;
import org.junit.jupiter.api.Test;

class CourseWithdrawalPolicyTests {

	@Test
	void belowSeventyPercentCanWithdrawWithoutRefund() {
		assertThat(CourseWithdrawalPolicy.refund(6_999, 10_000, BigInteger.valueOf(999_999))).isEqualTo(BigInteger.ZERO);
	}

	@Test
	void exactSeventyPercentReceivesFlooredEightyPercentOfUnusedAllocation() {
		assertThat(CourseWithdrawalPolicy.refund(7, 10, BigInteger.valueOf(1_000_001))).isEqualTo(BigInteger.valueOf(800_000));
	}

	@Test
	void noRemainingUnitMeansWithdrawalWindowIsClosed() {
		assertThat(CourseWithdrawalPolicy.canWithdraw(0, 10)).isFalse();
		assertThat(CourseWithdrawalPolicy.canWithdraw(1, 10)).isTrue();
	}

	@Test
	void scheduleConflictGetsFlooredRefundWithoutSeventyPercentThreshold() {
		var decision = CourseWithdrawalPolicy.evaluate(CourseWithdrawalCause.MEMBER_SCHEDULE_CONFLICT, 6, 10, BigInteger.valueOf(1_000_001));
		assertThat(decision.eligible()).isTrue();
		assertThat(decision.cause()).isEqualTo(CourseWithdrawalCause.MEMBER_SCHEDULE_CONFLICT);
		assertThat(decision.refundBasisVnd()).isEqualTo(BigInteger.valueOf(1_000_001));
		assertThat(decision.refundAmountVnd()).isEqualTo(BigInteger.valueOf(800_000));
	}

	@Test
	void voluntaryBelowThresholdRemainsEligibleWithZeroRefund() {
		var decision = CourseWithdrawalPolicy.evaluate(CourseWithdrawalCause.VOLUNTARY, 6, 10, BigInteger.valueOf(600_001));
		assertThat(decision.eligible()).isTrue();
		assertThat(decision.refundAmountVnd()).isZero();
		assertThat(decision.reason()).isEqualTo("ELIGIBLE_WITHOUT_REFUND");
	}
}
