package com.scms.backend.membership;

import java.time.Instant;

public record MemberReportResponse(Instant from, Instant to, long totalMembers, long activeMembers,
		long suspendedMembers, long activeMemberships, long expiredMemberships, boolean includesTestData) { }
