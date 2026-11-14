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
	void scheduleConflictGetsFullUnusedRefundWithoutSeventyPercentThreshold() {
		var decision = CourseWithdrawalPolicy.evaluate(CourseWithdrawalCause.MEMBER_SCHEDULE_CONFLICT, 6, 10, BigInteger.valueOf(1_000_001));
		assertThat(decision.eligible()).isTrue();
		assertThat(decision.cause()).isEqualTo(CourseWithdrawalCause.MEMBER_SCHEDULE_CONFLICT);
		assertThat(decision.refundBasisVnd()).isEqualTo(BigInteger.valueOf(1_000_001));
		assertThat(decision.refundAmountVnd()).isEqualTo(BigInteger.valueOf(1_000_001));
	}

	@org.junit.jupiter.params.ParameterizedTest
	@org.junit.jupiter.params.provider.EnumSource(value = CourseWithdrawalCause.class, names = { "CENTER_SESSION_RESCHEDULE", "MEMBER_SCHEDULE_CONFLICT" })
	void approvedFullRefundCausesReturnAllUnusedAllocationEvenForOneRemainingUnit(CourseWithdrawalCause cause) {
		assertThat(CourseWithdrawalPolicy.refund(cause, 1, 10, BigInteger.valueOf(123_457))).isEqualTo(BigInteger.valueOf(123_457));
		assertThat(CourseWithdrawalPolicy.refund(cause, 10, 10, BigInteger.valueOf(1_000_000))).isEqualTo(BigInteger.valueOf(1_000_000));
	}

	@org.junit.jupiter.params.ParameterizedTest
	@org.junit.jupiter.params.provider.EnumSource(value = CourseWithdrawalCause.class, names = { "START_DELAY_OVER_7_DAYS", "REDUCED_UNIT_COUNT" })
	void openingDelayAndReductionKeepEightyPercentWithoutThreshold(CourseWithdrawalCause cause) {
		assertThat(CourseWithdrawalPolicy.refund(cause, 1, 10, BigInteger.valueOf(123_457))).isEqualTo(BigInteger.valueOf(98_765));
	}

	@Test
	void voluntaryBelowThresholdRemainsEligibleWithZeroRefund() {
		var decision = CourseWithdrawalPolicy.evaluate(CourseWithdrawalCause.VOLUNTARY, 6, 10, BigInteger.valueOf(600_001));
		assertThat(decision.eligible()).isTrue();
		assertThat(decision.refundAmountVnd()).isZero();
		assertThat(decision.reason()).isEqualTo("ELIGIBLE_WITHOUT_REFUND");
	}
}
