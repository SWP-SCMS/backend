package com.scms.backend.membership;

import java.math.BigDecimal;
import java.time.Instant;

public record ClassUtilizationReportResponse(Instant from, Instant to, long totalSessions,
		long cancelledSessions, long reportableSessions, long totalCapacity, long booked, long cancelled,
		long present, long absent, BigDecimal utilizationRate, boolean includesTestData) { }
