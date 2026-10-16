package com.scms.backend.scheduling;

import java.time.Instant;

public record RoomSuspensionCreateRequest(Instant startTime, Instant endTime, String reason) {
}
