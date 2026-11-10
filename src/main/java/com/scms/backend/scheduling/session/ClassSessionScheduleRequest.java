package com.scms.backend.scheduling.session;

import java.time.Instant;
import java.util.UUID;

record ClassSessionScheduleRequest(Instant startTime, Instant endTime, UUID coachId, UUID roomId, Long expectedVersion, String reason) {
}
