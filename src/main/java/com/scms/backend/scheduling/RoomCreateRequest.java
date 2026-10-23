package com.scms.backend.scheduling;

public record RoomCreateRequest(String name, Integer capacity, RoomUsageMode usageMode) {
	public RoomCreateRequest(String name, Integer capacity) {
		this(name, capacity, RoomUsageMode.EXCLUSIVE);
	}
}
