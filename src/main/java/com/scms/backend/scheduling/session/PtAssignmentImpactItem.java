package com.scms.backend.scheduling.session;

import java.time.Instant;
import java.util.UUID;

public record PtAssignmentImpactItem(UUID id, UUID sessionId, Instant sessionStartTime) {
}
