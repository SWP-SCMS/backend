package com.scms.backend.audit;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRole;

import io.swagger.v3.oas.annotations.media.Schema;

public record AuditHistoryResponse(UUID id, UUID actorAccountId,
		@Schema(nullable = true, requiredMode = Schema.RequiredMode.REQUIRED) String actorName,
		@Schema(nullable = true, requiredMode = Schema.RequiredMode.REQUIRED) AccountRole actorRole,
		String action, String targetType, UUID targetId, String reason, Map<String, Object> beforeData,
		Map<String, Object> afterData, Instant createdAt) {

	static AuditHistoryResponse from(AuditEvent event, Account actor) {
		return new AuditHistoryResponse(event.getId(), event.getActorAccountId(),
			event.getActorNameSnapshot() != null ? event.getActorNameSnapshot()
				: actor == null ? null : actor.getFullName(),
			event.getActorRoleSnapshot() != null ? event.getActorRoleSnapshot()
				: actor == null ? null : actor.getRole(),
			event.getAction(), event.getTargetType(), event.getTargetId(), event.getReason(), event.getBeforeData(),
			event.getAfterData(), event.getCreatedAt());
	}
}
