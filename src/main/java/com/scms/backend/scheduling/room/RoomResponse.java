package com.scms.backend.scheduling.room;

import com.scms.backend.scheduling.Room;
import com.scms.backend.scheduling.RoomStatus;
import com.scms.backend.scheduling.RoomUsageMode;
import java.util.UUID;

public record RoomResponse(UUID id, String name, int capacity, RoomStatus status, RoomUsageMode usageMode) {

	static RoomResponse from(Room room) {
		return new RoomResponse(room.getId(), room.getName(), room.getCapacity(), room.getStatus(), room.getUsageMode());
	}
}
