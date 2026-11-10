package com.scms.backend.scheduling.room;

import com.scms.backend.scheduling.RoomUsageMode;

public record RoomCreateRequest(String name, Integer capacity, RoomUsageMode usageMode) {

	public RoomCreateRequest(String name, Integer capacity) {
		this(name, capacity, RoomUsageMode.EXCLUSIVE);
	}
}
