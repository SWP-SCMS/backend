package com.scms.backend.scheduling.session;

import com.scms.backend.scheduling.ClassSession;
import com.scms.backend.scheduling.ClassSessionStatus;
import java.time.Instant;
import java.util.UUID;

public record ClassSessionResponse(UUID id, UUID classId, UUID coachId, UUID roomId, Instant startTime, Instant endTime, int capacity, ClassSessionStatus status, long version, Long bookedCount) {

	public ClassSessionResponse(UUID id, UUID classId, UUID coachId, UUID roomId, Instant startTime, Instant endTime, int capacity, ClassSessionStatus status, long version) {
		this(id, classId, coachId, roomId, startTime, endTime, capacity, status, version, null);
	}

	ClassSessionResponse withBookedCount(long count) {
		return new ClassSessionResponse(id, classId, coachId, roomId, startTime, endTime, capacity, status, version, count);
	}

	static ClassSessionResponse from(ClassSession session) {
		return new ClassSessionResponse(session.getId(), session.getSportClass().getId(), session.getTeachingCoach().getId(), session.getRoom() == null ? null : session.getRoom().getId(), session.getStartTime(), session.getEndTime(), session.getCapacity(), session.getStatus(), session.getVersion() == null ? 0 : session.getVersion());
	}
}
