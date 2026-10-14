package com.scms.backend.training;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** US50 - Member - View Feedback. */
@RestController
@RequestMapping("/members/me/feedback")
public class MemberTrainingFeedbackController {

	private final MemberTrainingFeedbackService service;

	public MemberTrainingFeedbackController(MemberTrainingFeedbackService service) {
		this.service = service;
	}

	@GetMapping
	public ResponseEntity<List<MemberTrainingFeedbackResponse>> viewOwnHistory(
			@AuthenticationPrincipal Jwt jwt) {
		return ResponseEntity.ok(service.viewOwnHistory(UUID.fromString(jwt.getSubject())));
	}
}
