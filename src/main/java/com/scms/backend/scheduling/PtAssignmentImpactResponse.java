package com.scms.backend.scheduling;

import java.util.List;

public record PtAssignmentImpactResponse(
	int bookingCount,
	int waitlistCount,
	List<PtAssignmentImpactItem> bookings,
	List<PtAssignmentImpactItem> waitlists
) {
	public PtAssignmentImpactResponse {
		bookings = bookings == null ? List.of() : List.copyOf(bookings);
		waitlists = waitlists == null ? List.of() : List.copyOf(waitlists);
	}

	public static PtAssignmentImpactResponse empty() {
		return new PtAssignmentImpactResponse(0, 0, List.of(), List.of());
	}
}
