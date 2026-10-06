package com.scms.backend.scheduling;

import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/manager/class-sessions")
public class ClassSessionController {

	private final ClassSessionService service;

	ClassSessionController(ClassSessionService service) {
		this.service = service;
	}

	@PostMapping
	ResponseEntity<ClassSessionResponse> create(@AuthenticationPrincipal Jwt jwt,
			@RequestBody ClassSessionCreateRequest request) {
		return ResponseEntity.status(201).body(service.create(UUID.fromString(jwt.getSubject()), request));
	}

	@GetMapping
	ResponseEntity<ClassSessionPageResponse> list(@AuthenticationPrincipal Jwt jwt,
			@RequestParam(required = false) java.time.Instant from,
			@RequestParam(required = false) java.time.Instant to,
			@RequestParam(required = false) UUID classId,
			@RequestParam(required = false) UUID coachId,
			@RequestParam(required = false) UUID roomId,
			@RequestParam(required = false) ClassSessionStatus status,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		if (page < 0 || size < 1 || size > 100) {
			throw RecurringScheduleException.validation("page", "page must be >= 0 and size must be between 1 and 100");
		}
		var pageable = PageRequest.of(page, size,
			Sort.by("startTime").ascending().and(Sort.by("id")));
		return ResponseEntity.ok(service.list(UUID.fromString(jwt.getSubject()), from, to, classId, coachId,
			roomId, status, pageable));
	}

	@GetMapping("/{id}")
	ResponseEntity<ClassSessionDetailResponse> get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
		return ResponseEntity.ok(service.get(UUID.fromString(jwt.getSubject()), id));
	}
}
