package com.scms.backend.scheduling;

import java.time.Instant;
import java.util.UUID;

public record BookingScheduleResponse(
		UUID bookingId, UUID sessionId, UUID memberId, UUID membershipId, BookingStatus status,
		UUID bookedBy, Instant bookedAt, UUID cancelledBy, String cancellationSource, Instant cancelledAt,
		UUID classId, String className, SportClassType classType, UUID disciplineId, String disciplineName,
		UUID coachId, String coachName, UUID roomId, String roomName, Instant startTime, Instant endTime,
		int capacity, ClassSessionStatus sessionStatus) {

	static BookingScheduleResponse from(Booking booking, ClassSession session) {
		SportClass sportClass = session.getSportClass();
		Discipline discipline = sportClass.getDiscipline();
		return new BookingScheduleResponse(booking.getId(), booking.getClassSessionId(),
			booking.getMemberAccountId(), booking.getMembershipId(), booking.getStatus(),
			booking.getBookedByAccountId(), booking.getCreatedAt(), booking.getCancelledByAccountId(),
			booking.getCancellationSource(), booking.getCancelledAt(), sportClass.getId(), sportClass.getName(),
			sportClass.getClassType(), discipline.getId(), discipline.getName(), session.getTeachingCoach().getId(),
			session.getTeachingCoach().getFullName(), session.getRoom().getId(), session.getRoom().getName(),
			session.getStartTime(), session.getEndTime(), session.getCapacity(), session.getStatus());
	}
}
