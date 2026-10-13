package com.scms.backend.training;

import java.util.UUID;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;

/** US42-US43 - Personal Coach - Create and version Training Plans. */
@RestController
@RequestMapping("/coach/members/{memberId}/training-plans")
public class TrainingPlanController {

	private final TrainingPlanService service;

	public TrainingPlanController(TrainingPlanService service) {
		this.service = service;
	}

	@PostMapping
	public ResponseEntity<TrainingPlanResponse> create(@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID memberId, @Valid @RequestBody CreateTrainingPlanRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(service.create(UUID.fromString(jwt.getSubject()), memberId, request));
	}

	@PutMapping("/{planId}")
	public ResponseEntity<TrainingPlanResponse> update(@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID memberId, @PathVariable UUID planId,
			@Valid @RequestBody UpdateTrainingPlanRequest request) {
		return ResponseEntity.ok(service.update(UUID.fromString(jwt.getSubject()), memberId, planId, request));
	}
}
