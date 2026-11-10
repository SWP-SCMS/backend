package com.scms.backend.scheduling.session;

import java.util.UUID;

public record PtSessionAffectedWaitlist(UUID waitlistId, UUID memberAccountId, String memberCode, String memberFullName) {
}
