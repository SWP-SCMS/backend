package com.scms.backend.scheduling;

import java.time.Instant;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RoomSuspensionExtensionRequest(@NotNull Instant expectedEndTime, @NotNull Instant endTime,
		@NotBlank String reason,
		@NotNull List<RoomSessionResolutionRequest> resolutions) {

	public RoomSuspensionExtensionRequest {
		resolutions = resolutions == null ? null : List.copyOf(resolutions);
	}
}
