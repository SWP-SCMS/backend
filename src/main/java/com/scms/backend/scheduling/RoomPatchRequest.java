package com.scms.backend.scheduling;

public record RoomPatchRequest(String name, Integer capacity, RoomStatus status, RoomUsageMode usageMode) {
	public RoomPatchRequest(String name, Integer capacity, RoomStatus status) {
		this(name, capacity, status, null);
	}
}
