package com.scms.backend.scheduling;

import java.util.List;
import java.util.UUID;

public record PtAssignmentImpactConfirmation(List<UUID> bookingIds, List<UUID> waitlistIds) {
	public PtAssignmentImpactConfirmation {
		bookingIds = bookingIds == null ? List.of() : List.copyOf(bookingIds);
		waitlistIds = waitlistIds == null ? List.of() : List.copyOf(waitlistIds);
	}
}
