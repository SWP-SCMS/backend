package com.scms.backend.membership;

import java.math.BigInteger;
import java.time.Instant;

public record RevenueReportResponse(Instant from, Instant to, long paidPayments, BigInteger paidRevenue,
		BigInteger completedRefundAmount, BigInteger netRevenue,
		long pendingPayments, long failedPayments, long activeMemberships, long expiredMemberships,
		boolean includesTestData) { }
