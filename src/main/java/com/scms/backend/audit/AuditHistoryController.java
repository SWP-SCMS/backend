package com.scms.backend.audit;

import java.time.Instant;
import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/manager/audit-events")
class AuditHistoryController {

	private final AuditHistoryService service;

	AuditHistoryController(AuditHistoryService service) {
		this.service = service;
	}

	@GetMapping
	@Operation(summary = "Search append-only audit history")
	ResponseEntity<AuditHistoryPageResponse> list(@AuthenticationPrincipal Jwt jwt,
			@RequestParam(required = false) UUID actorId, @RequestParam(required = false) String action,
			@RequestParam(required = false) String targetType, @RequestParam(required = false) UUID targetId,
			@RequestParam(required = false) Instant from, @RequestParam(required = false) Instant to,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
		if (page < 0) {
			throw new AuditHistoryValidationException("page", "must be >= 0");
		}
		if (size < 1 || size > 100) {
			throw new AuditHistoryValidationException("size", "must be between 1 and 100");
		}
		Pageable pageable = PageRequest.of(page, size,
			Sort.by("createdAt").descending().and(Sort.by("id").descending()));
		return ResponseEntity.ok(service.list(UUID.fromString(jwt.getSubject()), actorId, action, targetType, targetId,
			from, to, pageable));
	}
}
