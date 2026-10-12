package com.scms.backend.audit;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

final class AuditHistorySpecifications {

	private AuditHistorySpecifications() {
	}

	static Specification<AuditEvent> filtered(UUID actorId, String action, String targetType, UUID targetId,
			Instant from, Instant to) {
		return (root, query, builder) -> {
			var predicates = builder.conjunction();
			if (actorId != null) {
				predicates = builder.and(predicates, builder.equal(root.get("actorAccountId"), actorId));
			}
			if (action != null) {
				predicates = builder.and(predicates, builder.equal(builder.upper(root.get("action")), action));
			}
			if (targetType != null) {
				predicates = builder.and(predicates, builder.equal(builder.upper(root.get("targetType")), targetType));
			}
			if (targetId != null) {
				predicates = builder.and(predicates, builder.equal(root.get("targetId"), targetId));
			}
			if (from != null) {
				predicates = builder.and(predicates, builder.greaterThanOrEqualTo(root.get("createdAt"), from));
			}
			if (to != null) {
				predicates = builder.and(predicates, builder.lessThan(root.get("createdAt"), to));
			}
			return predicates;
		};
	}
}
