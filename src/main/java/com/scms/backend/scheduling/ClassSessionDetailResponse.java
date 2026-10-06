package com.scms.backend.scheduling;

import java.time.Instant;
import java.util.UUID;

public record ClassSessionDetailResponse(UUID id, UUID classId, String className, SportClassType classType,
		UUID disciplineId, String disciplineName, UUID coachId, String coachName, UUID roomId, String roomName,
		int roomCapacity, UUID recurringScheduleId, Instant startTime, Instant endTime, int capacity,
		ClassSessionStatus status, Instant cancelledAt, String cancellationReason, UUID createdBy,
		boolean canUpdateAssignment, boolean canCancel) {

	static ClassSessionDetailResponse from(ClassSession session) {
		SportClass sportClass = session.getSportClass();
		Discipline discipline = sportClass.getDiscipline();
		Room room = session.getRoom();
		boolean scheduled = session.getStatus() == ClassSessionStatus.SCHEDULED;
		return new ClassSessionDetailResponse(session.getId(), sportClass.getId(), sportClass.getName(),
			sportClass.getClassType(), discipline.getId(), discipline.getName(), session.getTeachingCoach().getId(),
			session.getTeachingCoach().getFullName(), room.getId(), room.getName(), room.getCapacity(),
			session.getRecurringSchedule() == null ? null : session.getRecurringSchedule().getId(),
			session.getStartTime(), session.getEndTime(), session.getCapacity(), session.getStatus(),
			session.getCancelledAt(), session.getCancellationReason(), session.getCreatedBy(), scheduled, scheduled);
	}
}
