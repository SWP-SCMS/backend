package com.scms.backend.scheduling;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RoomImpactPreviewResponse(int count, List<SessionImpact> sessions,
		List<TargetRoom> bulkAvailableTargetRooms) {

	static RoomImpactPreviewResponse from(List<ClassSession> sessions, java.util.Map<UUID, Long> bookingCounts,
			java.util.Map<UUID, List<TargetRoom>> availableTargetRooms, List<TargetRoom> bulkAvailableTargetRooms) {
		List<SessionImpact> impacts = sessions.stream().map(session -> new SessionImpact(
			session.getId(), session.getSportClass().getId(), session.getSportClass().getName(),
			session.getTeachingCoach().getId(), session.getRoom().getId(), session.getRoom().getName(),
			session.getStartTime(), session.getEndTime(), session.getStatus(),
			bookingCounts.getOrDefault(session.getId(), 0L),
			availableTargetRooms.getOrDefault(session.getId(), List.of()))).toList();
		return new RoomImpactPreviewResponse(impacts.size(), impacts, bulkAvailableTargetRooms);
	}

	public record SessionImpact(UUID sessionId, UUID classId, String className, UUID coachId,
		UUID roomId, String roomName, Instant startTime, Instant endTime, ClassSessionStatus status,
		long bookingCount, List<TargetRoom> availableTargetRooms) {
	}

	public record TargetRoom(UUID roomId, String roomName, int capacity) {
		static TargetRoom from(RoomAvailabilityResponse room) {
			return new TargetRoom(room.roomId(), room.name(), room.capacity());
		}
	}
}
