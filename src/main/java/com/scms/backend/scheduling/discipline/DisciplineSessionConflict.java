package com.scms.backend.scheduling.discipline;

import java.time.Instant;
import java.util.UUID;

public record DisciplineSessionConflict(UUID sessionId, UUID classId, String className, Instant startTime, Instant endTime) {

	public static DisciplineSessionConflict from(DisciplineRepository.BlockingSessionView session) {
		return new DisciplineSessionConflict(session.getSessionId(), session.getClassId(), session.getClassName(), session.getStartTime(), session.getEndTime());
	}
}
