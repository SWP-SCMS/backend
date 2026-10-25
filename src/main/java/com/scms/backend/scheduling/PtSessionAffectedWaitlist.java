package com.scms.backend.scheduling;

import java.util.UUID;

public record PtSessionAffectedWaitlist(UUID waitlistId, UUID memberAccountId, String memberCode,
		String memberFullName) {
}
