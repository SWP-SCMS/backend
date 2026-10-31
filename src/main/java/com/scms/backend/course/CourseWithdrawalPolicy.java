package com.scms.backend.course;

import java.math.BigInteger;

final class CourseWithdrawalPolicy {
	private CourseWithdrawalPolicy() { }

	static boolean canWithdraw(int remainingUnits, int totalUnits) {
		return totalUnits > 0 && remainingUnits > 0 && remainingUnits <= totalUnits;
	}

	static BigInteger refund(int remainingUnits, int totalUnits, BigInteger unusedAllocation) {
		if (!canWithdraw(remainingUnits, totalUnits) || unusedAllocation == null || unusedAllocation.signum() < 0)
			throw new IllegalArgumentException("Invalid withdrawal allocation.");
		if ((long) remainingUnits * 100 < (long) totalUnits * 70) return BigInteger.ZERO;
		return unusedAllocation.multiply(BigInteger.valueOf(80)).divide(BigInteger.valueOf(100));
	}
}
