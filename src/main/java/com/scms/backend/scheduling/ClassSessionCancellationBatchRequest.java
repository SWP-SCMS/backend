package com.scms.backend.scheduling;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ClassSessionCancellationBatchRequest(List<UUID> sessionIds, UUID classId, Instant from, Instant to,
		String reason) {

	public ClassSessionCancellationBatchRequest(List<UUID> sessionIds, String reason) {
		this(sessionIds, null, null, null, reason);
	}
}
