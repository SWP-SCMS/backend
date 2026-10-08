package com.scms.backend.scheduling;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/manager/classes")
public class SportClassController {

	private final SportClassService service;

	SportClassController(SportClassService service) {
		this.service = service;
	}

	@GetMapping
	ResponseEntity<List<SportClassResponse>> list(@AuthenticationPrincipal Jwt jwt) {
		return ResponseEntity.ok(service.list(accountId(jwt)));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	ResponseEntity<SportClassResponse> create(@AuthenticationPrincipal Jwt jwt,
			@RequestBody SportClassCreateRequest request) {
		return ResponseEntity.status(201).body(service.create(accountId(jwt), request));
	}

	@GetMapping("/{classId}")
	ResponseEntity<SportClassResponse> get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID classId) {
		return ResponseEntity.ok(service.get(accountId(jwt), classId));
	}

	@PatchMapping("/{classId}")
	ResponseEntity<SportClassResponse> update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID classId,
			@RequestBody SportClassPatchRequest request) {
		return ResponseEntity.ok(service.update(accountId(jwt), classId, request));
	}

	private UUID accountId(Jwt jwt) {
		return UUID.fromString(jwt.getSubject());
	}
}
