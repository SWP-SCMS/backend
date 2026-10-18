package com.scms.backend.training;

import java.util.UUID;

import jakarta.validation.Valid;
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

	@GetMapping
	public ResponseEntity<TrainingResultPageResponse> list(@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID memberId, @RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		validatePage(page, size);
		var pageable = PageRequest.of(page, size,
				Sort.by(Sort.Order.desc("activityAt"), Sort.Order.desc("id")));
		return ResponseEntity.ok(service.list(UUID.fromString(jwt.getSubject()), memberId, pageable));
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

	private void validatePage(int page, int size) {
		if (page < 0 || size < 1 || size > 100) {
			throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST,
					"page must be >= 0 and size must be between 1 and 100");
		}
	}
}
