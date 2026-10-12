package com.scms.backend.membership;

import java.time.Instant;

public record ManagerDashboardResponse(Instant from, Instant to, MemberReportResponse members,
		RevenueReportResponse membershipRevenue, BookingReportResponse bookings,
		AttendanceReportResponse attendance, CenterVisitReportResponse centerVisits,
		ClassUtilizationReportResponse classUtilization, boolean includesTestData) { }
