package com.scms.backend.scheduling.room;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;

public record RoomSuspensionExtensionRequest(@NotNull Instant expectedEndTime, @NotNull Instant endTime, @NotBlank String reason, List<RoomSessionResolutionRequest> resolutions, RoomBulkResolutionRequest bulkResolution, List<RoomSessionResolutionRequest> resolutionOverrides) {

	public RoomSuspensionExtensionRequest(Instant expectedEndTime, Instant endTime, String reason, List<RoomSessionResolutionRequest> resolutions) {
		this(expectedEndTime, endTime, reason, resolutions, null, List.of());
	}

	public RoomSuspensionExtensionRequest {
		resolutions = resolutions == null ? null : List.copyOf(resolutions);
		resolutionOverrides = resolutionOverrides == null ? List.of() : List.copyOf(resolutionOverrides);
	}
}
