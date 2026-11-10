package com.scms.backend.membership.reporting;

import java.time.Instant;

public record BookingReportResponse(Instant from, Instant to, long totalBookings, long bookedBookings, long cancelledBookings, boolean includesTestData) {
}
