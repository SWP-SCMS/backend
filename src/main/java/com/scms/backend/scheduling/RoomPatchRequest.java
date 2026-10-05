package com.scms.backend.scheduling;

public record RoomPatchRequest(String name, Integer capacity, RoomStatus status) {
}
