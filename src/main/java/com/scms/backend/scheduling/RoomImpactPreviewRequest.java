package com.scms.backend.scheduling;

import java.time.Instant;
import java.util.List;

public record RoomImpactPreviewRequest(Instant startTime, Instant endTime, boolean permanentDisable,
		String reason, List<RoomSessionResolutionRequest> resolutions) {

	public RoomImpactPreviewRequest(Instant startTime, Instant endTime, boolean permanentDisable) {
		this(startTime, endTime, permanentDisable, null, List.of());
	}

	public RoomImpactPreviewRequest {
		resolutions = resolutions == null ? List.of() : resolutions;
	}
}
