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

/** US48 - Personal Coach - Create / Edit Feedback. */
@RestController
@RequestMapping("/coach/members/{memberId}/feedback")
public class TrainingFeedbackController {

	private final TrainingFeedbackService service;

	public TrainingFeedbackController(TrainingFeedbackService service) {
		this.service = service;
	}

	@PostMapping
	public ResponseEntity<TrainingFeedbackResponse> create(@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID memberId, @Valid @RequestBody PersonalTrainingFeedbackRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(service.create(UUID.fromString(jwt.getSubject()), memberId, request));
	}

	@PutMapping("/{feedbackId}")
	public ResponseEntity<TrainingFeedbackResponse> update(@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID memberId, @PathVariable UUID feedbackId,
			@Valid @RequestBody PersonalTrainingFeedbackRequest request) {
		return ResponseEntity.ok(service.update(UUID.fromString(jwt.getSubject()), memberId, feedbackId, request));
	}
}
