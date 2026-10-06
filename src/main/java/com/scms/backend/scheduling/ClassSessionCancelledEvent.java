package com.scms.backend.scheduling;

import java.util.List;
import java.util.UUID;

public record ClassSessionCancelledEvent(UUID sessionId, List<UUID> memberIds) {
	public ClassSessionCancelledEvent {
		memberIds = List.copyOf(memberIds);
	}
}
