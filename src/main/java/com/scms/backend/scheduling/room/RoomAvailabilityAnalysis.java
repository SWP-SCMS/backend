package com.scms.backend.scheduling.room;

import com.scms.backend.scheduling.ClassSession;
import com.scms.backend.scheduling.RoomUsageMode;
import com.scms.backend.scheduling.room.RoomSuspensionService.CapacityPoint;
import com.scms.backend.scheduling.room.RoomSuspensionService.TargetRoomAvailability;
import com.scms.backend.scheduling.session.ClassSessionRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

final class RoomAvailabilityAnalysis {

	private final RoomRepository rooms;

	private final ClassSessionRepository sessions;

	private final RoomService roomService;

	RoomAvailabilityAnalysis(RoomRepository rooms, ClassSessionRepository sessions, RoomService roomService) {
		this.rooms = rooms;
		this.sessions = sessions;
		this.roomService = roomService;
	}

	TargetRoomAvailability targetRoomAvailability(UUID sourceRoomId, List<ClassSession> affected) {
		if (affected.isEmpty())
			return new TargetRoomAvailability(Map.of(), List.of());
		Map<UUID, List<RoomAvailabilityResponse>> details = new HashMap<>();
		Map<UUID, List<RoomImpactPreviewResponse.TargetRoom>> bySession = new HashMap<>();
		for (ClassSession session : affected) details.put(session.getId(), new ArrayList<>());
		Map<UUID, Integer> requestedCapacity = new HashMap<>();
		for (ClassSession session : affected) requestedCapacity.put(session.getId(), session.getCapacity());
		for (RoomSessionAvailabilityView row : rooms.findBatchAvailability(sourceRoomId, affected.stream().map(ClassSession::getId).toList())) {
			RoomAvailabilityResponse room = roomService.availabilityResponse(row, requestedCapacity.get(row.getSessionId()));
			if (room.available())
				details.get(row.getSessionId()).add(room);
		}
		for (ClassSession session : affected) {
			bySession.put(session.getId(), details.get(session.getId()).stream().map(RoomImpactPreviewResponse.TargetRoom::from).toList());
		}
		Set<UUID> commonRoomIds = new java.util.LinkedHashSet<>(details.get(affected.getFirst().getId()).stream().map(RoomAvailabilityResponse::roomId).toList());
		for (int index = 1; index < affected.size() && !commonRoomIds.isEmpty(); index++) {
			Set<UUID> sessionRoomIds = details.get(affected.get(index).getId()).stream().map(RoomAvailabilityResponse::roomId).collect(java.util.stream.Collectors.toSet());
			commonRoomIds.retainAll(sessionRoomIds);
		}
		Map<UUID, Long> overlappingCapacity = overlappingCapacity(affected);
		boolean overlappingSessions = hasOverlappingSessions(affected);
		List<RoomImpactPreviewResponse.TargetRoom> bulk = details.get(affected.getFirst().getId()).stream().filter(room -> commonRoomIds.contains(room.roomId())).filter(room -> supportsBulkMove(room, affected, details, overlappingCapacity, overlappingSessions)).map(RoomImpactPreviewResponse.TargetRoom::from).toList();
		return new TargetRoomAvailability(Map.copyOf(bySession), bulk);
	}

	boolean supportsBulkMove(RoomAvailabilityResponse candidate, List<ClassSession> affected, Map<UUID, List<RoomAvailabilityResponse>> details, Map<UUID, Long> overlappingCapacity, boolean overlappingSessions) {
		if (candidate.usageMode() == RoomUsageMode.EXCLUSIVE) {
			return !overlappingSessions;
		}
		for (ClassSession anchor : affected) {
			RoomAvailabilityResponse window = details.get(anchor.getId()).stream().filter(room -> room.roomId().equals(candidate.roomId())).findFirst().orElseThrow();
			if (window.occupiedCapacity() + overlappingCapacity.get(anchor.getId()) > candidate.capacity())
				return false;
		}
		return true;
	}

	Map<UUID, Long> overlappingCapacity(List<ClassSession> sessions) {
		List<CapacityPoint> starts = sessions.stream().map(session -> new CapacityPoint(session.getStartTime(), session.getCapacity())).sorted(Comparator.comparing(CapacityPoint::time)).toList();
		List<CapacityPoint> ends = sessions.stream().map(session -> new CapacityPoint(session.getEndTime(), session.getCapacity())).sorted(Comparator.comparing(CapacityPoint::time)).toList();
		long[] startPrefix = capacityPrefix(starts);
		long[] endPrefix = capacityPrefix(ends);
		Map<UUID, Long> result = new HashMap<>();
		for (ClassSession session : sessions) {
			int started = lowerBound(starts, session.getEndTime());
			int ended = upperBound(ends, session.getStartTime());
			result.put(session.getId(), startPrefix[started] - endPrefix[ended]);
		}
		return result;
	}

	long[] capacityPrefix(List<CapacityPoint> points) {
		long[] prefix = new long[points.size() + 1];
		for (int index = 0; index < points.size(); index++) {
			prefix[index + 1] = prefix[index] + points.get(index).capacity();
		}
		return prefix;
	}

	int lowerBound(List<CapacityPoint> points, Instant time) {
		int low = 0;
		int high = points.size();
		while (low < high) {
			int middle = (low + high) >>> 1;
			if (points.get(middle).time().isBefore(time))
				low = middle + 1;
			else
				high = middle;
		}
		return low;
	}

	int upperBound(List<CapacityPoint> points, Instant time) {
		int low = 0;
		int high = points.size();
		while (low < high) {
			int middle = (low + high) >>> 1;
			if (!points.get(middle).time().isAfter(time))
				low = middle + 1;
			else
				high = middle;
		}
		return low;
	}

	boolean hasOverlappingSessions(List<ClassSession> sessions) {
		Instant latestEnd = null;
		for (ClassSession session : sessions.stream().sorted(Comparator.comparing(ClassSession::getStartTime)).toList()) {
			if (latestEnd != null && session.getStartTime().isBefore(latestEnd))
				return true;
			if (latestEnd == null || session.getEndTime().isAfter(latestEnd))
				latestEnd = session.getEndTime();
		}
		return false;
	}
}
