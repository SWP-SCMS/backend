package com.scms.backend.membership.reporting;

import java.time.Instant;

public record CenterRevenueReportResponse(Instant from, Instant to, RevenueSourceResponse membership,
		RevenueSourceResponse course, RevenueSourceResponse total, boolean includesTestData) {
}
