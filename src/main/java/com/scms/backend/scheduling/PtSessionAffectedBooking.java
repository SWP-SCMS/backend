package com.scms.backend.scheduling;

import java.util.UUID;

public record PtSessionAffectedBooking(UUID bookingId, UUID memberAccountId, String memberCode,
		String memberFullName) {
}
