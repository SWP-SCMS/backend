package com.scms.backend.scheduling;

import java.time.Instant;
import java.util.List;

public record RoomImpactPreviewRequest(Instant startTime, Instant endTime, boolean permanentDisable,
		String reason, List<RoomSessionResolutionRequest> resolutions,
		RoomBulkResolutionRequest bulkResolution,
		List<RoomSessionResolutionRequest> resolutionOverrides) {

	public RoomImpactPreviewRequest(Instant startTime, Instant endTime, boolean permanentDisable) {
		this(startTime, endTime, permanentDisable, null, List.of(), null, List.of());
	}

	public RoomImpactPreviewRequest(Instant startTime, Instant endTime, boolean permanentDisable,
			String reason, List<RoomSessionResolutionRequest> resolutions) {
		this(startTime, endTime, permanentDisable, reason, resolutions, null, List.of());
	}

	public RoomImpactPreviewRequest {
		resolutions = resolutions == null ? List.of() : List.copyOf(resolutions);
		resolutionOverrides = resolutionOverrides == null ? List.of() : List.copyOf(resolutionOverrides);
	}
}
