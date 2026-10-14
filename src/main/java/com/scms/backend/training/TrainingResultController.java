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

/** US45 - Personal Coach - Record Personal Training Result. */
@RestController
@RequestMapping("/coach/members/{memberId}/training-results")
public class TrainingResultController {

	private final TrainingResultService service;

	public TrainingResultController(TrainingResultService service) {
		this.service = service;
	}

	@PostMapping
	public ResponseEntity<TrainingResultResponse> create(@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID memberId, @Valid @RequestBody PersonalTrainingResultRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(service.create(UUID.fromString(jwt.getSubject()), memberId, request));
	}

	@PutMapping("/{resultId}")
	public ResponseEntity<TrainingResultResponse> update(@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID memberId, @PathVariable UUID resultId,
			@Valid @RequestBody PersonalTrainingResultRequest request) {
		return ResponseEntity.ok(service.update(UUID.fromString(jwt.getSubject()), memberId, resultId, request));
	}
}
