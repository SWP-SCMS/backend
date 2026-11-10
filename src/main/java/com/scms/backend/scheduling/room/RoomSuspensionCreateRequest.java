package com.scms.backend.scheduling.room;

import java.time.Instant;

public record RoomSuspensionCreateRequest(Instant startTime, Instant endTime, String reason) {
}
