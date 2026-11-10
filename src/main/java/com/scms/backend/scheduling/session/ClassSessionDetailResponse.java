package com.scms.backend.scheduling.session;

import com.scms.backend.scheduling.ClassSession;
import com.scms.backend.scheduling.ClassSessionStatus;
import com.scms.backend.scheduling.Discipline;
import com.scms.backend.scheduling.Room;
import com.scms.backend.scheduling.SportClass;
import com.scms.backend.scheduling.SportClassType;
import java.time.Instant;
import java.util.UUID;

public record ClassSessionDetailResponse(UUID id, UUID classId, String className, SportClassType classType, UUID disciplineId, String disciplineName, UUID coachId, String coachName, UUID roomId, String roomName, int roomCapacity, UUID recurringScheduleId, Instant startTime, Instant endTime, int capacity, ClassSessionStatus status, Instant cancelledAt, String cancellationReason, UUID createdBy, boolean canUpdateAssignment, boolean canCancel, long version) {

	static ClassSessionDetailResponse from(ClassSession session) {
		SportClass sportClass = session.getSportClass();
		Discipline discipline = sportClass.getDiscipline();
		Room room = session.getRoom();
		boolean scheduled = session.getStatus() == ClassSessionStatus.SCHEDULED;
		return new ClassSessionDetailResponse(session.getId(), sportClass.getId(), sportClass.getName(), sportClass.getClassType(), discipline.getId(), discipline.getName(), session.getTeachingCoach().getId(), session.getTeachingCoach().getFullName(), room == null ? null : room.getId(), room == null ? null : room.getName(), room == null ? 0 : room.getCapacity(), session.getRecurringSchedule() == null ? null : session.getRecurringSchedule().getId(), session.getStartTime(), session.getEndTime(), session.getCapacity(), session.getStatus(), session.getCancelledAt(), session.getCancellationReason(), session.getCreatedBy(), scheduled, scheduled, session.getVersion() == null ? 0 : session.getVersion());
	}
}
