package com.scms.backend.scheduling.room;

import com.scms.backend.scheduling.RoomStatus;
import com.scms.backend.scheduling.RoomUsageMode;

public record RoomPatchRequest(String name, Integer capacity, RoomStatus status, RoomUsageMode usageMode) {

	public RoomPatchRequest(String name, Integer capacity, RoomStatus status) {
		this(name, capacity, status, null);
	}
}
