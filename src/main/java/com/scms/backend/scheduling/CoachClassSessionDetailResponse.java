package com.scms.backend.scheduling;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CoachClassSessionDetailResponse(UUID id, UUID classId, String className,
		SportClassType classType, UUID disciplineId, String disciplineName, UUID coachId, String coachName,
		UUID roomId, String roomName, Instant startTime, Instant endTime, int capacity,
		ClassSessionStatus status, Instant cancelledAt, String cancellationReason,
		List<CoachSessionMemberResponse> members) {

	static CoachClassSessionDetailResponse from(ClassSession session, List<CoachSessionMemberResponse> members) {
		SportClass sportClass = session.getSportClass();
		return new CoachClassSessionDetailResponse(session.getId(), sportClass.getId(), sportClass.getName(),
			sportClass.getClassType(), sportClass.getDiscipline().getId(), sportClass.getDiscipline().getName(),
			session.getTeachingCoach().getId(), session.getTeachingCoach().getFullName(), session.getRoom().getId(),
			session.getRoom().getName(), session.getStartTime(), session.getEndTime(), session.getCapacity(),
			session.getStatus(), session.getCancelledAt(), session.getCancellationReason(), members);
	}
}
