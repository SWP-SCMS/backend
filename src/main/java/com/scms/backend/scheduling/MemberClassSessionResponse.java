package com.scms.backend.scheduling;

import java.time.Instant;
import java.util.UUID;

public record MemberClassSessionResponse(UUID id, UUID classId, String className, SportClassType classType,
		UUID disciplineId, String disciplineName, UUID coachId, String coachName, UUID roomId, String roomName,
		Instant startTime, Instant endTime, int capacity, long bookedCount, int availableCapacity,
		ClassSessionStatus status, boolean bookingAvailable) {

	static MemberClassSessionResponse from(ClassSession session, long bookedCount, Instant now) {
		SportClass sportClass = session.getSportClass();
		int available = Math.max(0, session.getCapacity() - Math.toIntExact(bookedCount));
		boolean bookable = session.getStatus() == ClassSessionStatus.SCHEDULED
			&& session.getStartTime().isAfter(now) && available > 0;
		return new MemberClassSessionResponse(session.getId(), sportClass.getId(), sportClass.getName(),
			sportClass.getClassType(), sportClass.getDiscipline().getId(), sportClass.getDiscipline().getName(),
			session.getTeachingCoach().getId(), session.getTeachingCoach().getFullName(), session.getRoom().getId(),
			session.getRoom().getName(), session.getStartTime(), session.getEndTime(), session.getCapacity(),
			bookedCount, available, session.getStatus(), bookable);
	}
}
