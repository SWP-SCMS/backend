package com.scms.backend.membership.reporting;

import java.math.BigInteger;

public record RevenueSourceResponse(long paidPayments, BigInteger paidRevenue, BigInteger completedRefundAmount,
		BigInteger netRevenue, long pendingPayments, long failedPayments, BigInteger pendingRefundAmount) {

	static RevenueSourceResponse membership(RevenueReportResponse report, BigInteger pendingRefundAmount) {
		return new RevenueSourceResponse(report.paidPayments(), report.paidRevenue(), report.completedRefundAmount(),
			report.netRevenue(), report.pendingPayments(), report.failedPayments(), pendingRefundAmount);
	}

	RevenueSourceResponse plus(RevenueSourceResponse other) {
		return new RevenueSourceResponse(paidPayments + other.paidPayments, paidRevenue.add(other.paidRevenue),
			completedRefundAmount.add(other.completedRefundAmount), netRevenue.add(other.netRevenue),
			pendingPayments + other.pendingPayments, failedPayments + other.failedPayments,
			pendingRefundAmount.add(other.pendingRefundAmount));
	}
}
