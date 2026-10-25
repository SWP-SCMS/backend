package com.scms.backend.scheduling;

import java.util.UUID;

public record RoomAvailabilityResponse(UUID roomId, String name, RoomUsageMode usageMode, int capacity,
		long occupiedCapacity, long availableCapacity, boolean available) {
}
