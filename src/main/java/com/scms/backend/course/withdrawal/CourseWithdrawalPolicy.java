package com.scms.backend.course.withdrawal;

import com.scms.backend.course.CourseWithdrawalCause;
import java.math.BigInteger;

public final class CourseWithdrawalPolicy {

	private CourseWithdrawalPolicy() {
	}

	public static boolean canWithdraw(int remainingUnits, int totalUnits) {
		return totalUnits > 0 && remainingUnits > 0 && remainingUnits <= totalUnits;
	}

	public static BigInteger refund(int remainingUnits, int totalUnits, BigInteger unusedAllocation) {
		return refund(CourseWithdrawalCause.VOLUNTARY, remainingUnits, totalUnits, unusedAllocation);
	}

	public static BigInteger refund(CourseWithdrawalCause cause, int remainingUnits, int totalUnits, BigInteger unusedAllocation) {
		if (!canWithdraw(remainingUnits, totalUnits) || unusedAllocation == null || unusedAllocation.signum() < 0)
			throw new IllegalArgumentException("Invalid withdrawal allocation.");
		if (cause == null)
			throw new IllegalArgumentException("Withdrawal cause is required.");
		if (cause.fullUnusedRefund())
			return unusedAllocation;
		if (cause == CourseWithdrawalCause.VOLUNTARY && (long) remainingUnits * 100 < (long) totalUnits * 70)
			return BigInteger.ZERO;
		return unusedAllocation.multiply(BigInteger.valueOf(80)).divide(BigInteger.valueOf(100));
	}

	public static Decision evaluate(CourseWithdrawalCause cause, int remainingUnits, int totalUnits, BigInteger unusedAllocation) {
		if (!canWithdraw(remainingUnits, totalUnits))
			return new Decision(false, "FINAL_UNIT_ALREADY_STARTED", cause, BigInteger.ZERO, BigInteger.ZERO);
		BigInteger basis = unusedAllocation == null ? BigInteger.ZERO : unusedAllocation.max(BigInteger.ZERO);
		BigInteger amount = refund(cause, remainingUnits, totalUnits, basis);
		String reason = amount.signum() == 0 && cause == CourseWithdrawalCause.VOLUNTARY ? "ELIGIBLE_WITHOUT_REFUND" : "ELIGIBLE_WITH_REFUND";
		return new Decision(true, reason, cause, basis, amount);
	}

	public record Decision(boolean eligible, String reason, CourseWithdrawalCause cause, BigInteger refundBasisVnd, BigInteger refundAmountVnd) {
	}
}
