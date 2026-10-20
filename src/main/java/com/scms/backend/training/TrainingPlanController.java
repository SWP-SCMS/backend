package com.scms.backend.training;

import java.util.UUID;
import java.util.List;

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

	@GetMapping
	public ResponseEntity<TrainingPlanPageResponse> list(@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID memberId, @RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		validatePage(page, size);
		var pageable = PageRequest.of(page, size,
				Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
		return ResponseEntity.ok(service.list(UUID.fromString(jwt.getSubject()), memberId, pageable));
	}

	@PostMapping
	public ResponseEntity<TrainingPlanResponse> create(@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID memberId, @Valid @RequestBody CreateTrainingPlanRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(service.create(UUID.fromString(jwt.getSubject()), memberId, request));
	}

	@PutMapping("/{planId}")
	@Operation(description = "Requires the TRAINING_PLAN Benefit. The current assigned Coach may append a version to a plan created under an earlier assignment. Each version records its creator; expectedVersionNumber prevents stale updates.")
	public ResponseEntity<TrainingPlanResponse> update(@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID memberId, @PathVariable UUID planId,
			@Valid @RequestBody UpdateTrainingPlanRequest request) {
		return ResponseEntity.ok(service.update(UUID.fromString(jwt.getSubject()), memberId, planId, request));
	}

	@GetMapping("/{planId}/versions")
	@Operation(description = "Returns immutable versions newest first. Requires the active Coach assignment and verifies that planId belongs to memberId.")
	public ResponseEntity<List<TrainingPlanVersionResponse>> versions(@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID memberId, @PathVariable UUID planId) {
		return ResponseEntity.ok(service.versions(UUID.fromString(jwt.getSubject()), memberId, planId));
	}

	@PostMapping("/{planId}/archive")
	@Operation(description = "Archives an ACTIVE plan without deleting its versions. Repeated requests are idempotent; restore is not supported.")
	public ResponseEntity<TrainingPlanResponse> archive(@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID memberId, @PathVariable UUID planId) {
		return ResponseEntity.ok(service.archive(UUID.fromString(jwt.getSubject()), memberId, planId));
	}

	private void validatePage(int page, int size) {
		if (page < 0 || size < 1 || size > 100) {
			throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST,
					"page must be >= 0 and size must be between 1 and 100");
		}
	}
}
