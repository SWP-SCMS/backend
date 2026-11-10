package com.scms.backend.membership.reporting;

import java.math.BigDecimal;
import java.time.Instant;

public record AttendanceReportResponse(Instant from, Instant to, long reportableSessions, long present, long absent, BigDecimal participationRate, boolean includesTestData) {
}
