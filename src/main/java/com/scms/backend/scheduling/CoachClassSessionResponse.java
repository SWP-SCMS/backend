package com.scms.backend.scheduling;

import java.time.Instant;
import java.util.UUID;

public record CoachClassSessionResponse(UUID id, UUID classId, String className,
		SportClassType classType, UUID disciplineId, String disciplineName, UUID coachId, String coachName,
		UUID roomId, String roomName, Instant startTime, Instant endTime, int capacity, long bookedCount,
		ClassSessionStatus status, long version) {

	static CoachClassSessionResponse from(ClassSession session, long bookedCount) {
		SportClass sportClass = session.getSportClass();
		return new CoachClassSessionResponse(session.getId(), sportClass.getId(), sportClass.getName(),
			sportClass.getClassType(), sportClass.getDiscipline().getId(), sportClass.getDiscipline().getName(),
			session.getTeachingCoach().getId(), session.getTeachingCoach().getFullName(),
			session.getRoom() == null ? null : session.getRoom().getId(),
			session.getRoom() == null ? null : session.getRoom().getName(), session.getStartTime(),
			session.getEndTime(), session.getCapacity(), bookedCount, session.getStatus(),
			session.getVersion() == null ? 0 : session.getVersion());
	}
}
