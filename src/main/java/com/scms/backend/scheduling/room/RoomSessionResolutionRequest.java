package com.scms.backend.scheduling.room;

import java.time.Instant;
import java.util.UUID;

public record RoomSessionResolutionRequest(UUID sessionId, String action, UUID targetRoomId, Instant startTime, Instant endTime, String reason) {
}
