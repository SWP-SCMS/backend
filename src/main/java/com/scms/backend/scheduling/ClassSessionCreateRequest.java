package com.scms.backend.scheduling;

import java.time.Instant;
import java.util.UUID;

public record ClassSessionCreateRequest(UUID classId, UUID coachId, UUID roomId,
		Instant startTime, Instant endTime, Integer capacity) {
}
