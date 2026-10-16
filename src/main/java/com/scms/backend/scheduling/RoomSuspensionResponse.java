package com.scms.backend.scheduling;

import java.time.Instant;
import java.util.UUID;

public record RoomSuspensionResponse(UUID id, UUID roomId, Instant startTime, Instant endTime, String reason,
		UUID createdBy) {

	static RoomSuspensionResponse from(RoomSuspension suspension) {
		return new RoomSuspensionResponse(suspension.getId(), suspension.getRoom().getId(),
			suspension.getStartTime(), suspension.getEndTime(), suspension.getReason(), suspension.getCreatedBy());
	}
}
