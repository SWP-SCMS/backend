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

/** US49 - Teaching Coach - Group / Yoga Session Feedback. */
@RestController
@RequestMapping("/coach/class-sessions/{sessionId}/members/{memberId}/feedback")
public class SessionTrainingFeedbackController {

	private final SessionTrainingFeedbackService service;

	public SessionTrainingFeedbackController(SessionTrainingFeedbackService service) {
		this.service = service;
	}

	@PostMapping
	public ResponseEntity<SessionTrainingFeedbackResponse> create(@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID sessionId, @PathVariable UUID memberId,
			@Valid @RequestBody SessionTrainingFeedbackRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED)
			.body(service.create(UUID.fromString(jwt.getSubject()), sessionId, memberId, request));
	}

	@PutMapping("/{feedbackId}")
	public ResponseEntity<SessionTrainingFeedbackResponse> update(@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID sessionId, @PathVariable UUID memberId, @PathVariable UUID feedbackId,
			@Valid @RequestBody SessionTrainingFeedbackRequest request) {
		return ResponseEntity.ok(service.update(UUID.fromString(jwt.getSubject()), sessionId, memberId,
			feedbackId, request));
	}
}
