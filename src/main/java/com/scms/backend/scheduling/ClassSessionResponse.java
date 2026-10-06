package com.scms.backend.scheduling;

import java.time.Instant;
import java.util.UUID;

public record ClassSessionResponse(UUID id, UUID classId, UUID coachId, UUID roomId,
		Instant startTime, Instant endTime, int capacity, ClassSessionStatus status) {

	static ClassSessionResponse from(ClassSession session) {
		return new ClassSessionResponse(session.getId(), session.getSportClass().getId(),
			session.getTeachingCoach().getId(), session.getRoom().getId(), session.getStartTime(),
			session.getEndTime(), session.getCapacity(), session.getStatus());
	}
}
