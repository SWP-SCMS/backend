package com.scms.backend.training;

import java.util.UUID;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** US46 - Teaching Coach - Record Group / Yoga Session Result. */
@RestController
@RequestMapping("/coach/class-sessions/{sessionId}/members/{memberId}/training-results")
public class SessionTrainingResultController {

	private final SessionTrainingResultService service;

	public SessionTrainingResultController(SessionTrainingResultService service) {
		this.service = service;
	}

	@PostMapping
	public ResponseEntity<SessionTrainingResultResponse> create(@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID sessionId, @PathVariable UUID memberId,
			@Valid @RequestBody SessionTrainingResultRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED)
			.body(service.create(UUID.fromString(jwt.getSubject()), sessionId, memberId, request));
	}

	@PutMapping("/{resultId}")
	public ResponseEntity<SessionTrainingResultResponse> update(@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID sessionId, @PathVariable UUID memberId, @PathVariable UUID resultId,
			@Valid @RequestBody SessionTrainingResultRequest request) {
		return ResponseEntity.ok(service.update(UUID.fromString(jwt.getSubject()), sessionId, memberId,
			resultId, request));
	}
}
