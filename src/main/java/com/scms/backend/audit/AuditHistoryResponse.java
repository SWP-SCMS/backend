package com.scms.backend.audit;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AuditHistoryResponse(UUID id, UUID actorAccountId, String action, String targetType, UUID targetId,
		String reason, Map<String, Object> beforeData, Map<String, Object> afterData, Instant createdAt) {

	static AuditHistoryResponse from(AuditEvent event) {
		return new AuditHistoryResponse(event.getId(), event.getActorAccountId(), event.getAction(),
			event.getTargetType(), event.getTargetId(), event.getReason(), event.getBeforeData(), event.getAfterData(),
			event.getCreatedAt());
	}
}
