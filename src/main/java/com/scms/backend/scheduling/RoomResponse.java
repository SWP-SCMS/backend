package com.scms.backend.scheduling;

import java.util.UUID;

public record RoomResponse(UUID id, String name, int capacity, RoomStatus status) {

	static RoomResponse from(Room room) {
		return new RoomResponse(room.getId(), room.getName(), room.getCapacity(), room.getStatus());
	}
}
