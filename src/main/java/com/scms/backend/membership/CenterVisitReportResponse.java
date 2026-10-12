package com.scms.backend.membership;

import java.time.Instant;

public record CenterVisitReportResponse(Instant from, Instant to, long totalVisits, long uniqueMembers,
		long openVisits, boolean includesTestData) { }
