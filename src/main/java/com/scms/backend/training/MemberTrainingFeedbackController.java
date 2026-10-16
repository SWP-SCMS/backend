package com.scms.backend.training;

import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
	public ResponseEntity<MemberTrainingFeedbackPageResponse> viewOwnHistory(
			@AuthenticationPrincipal Jwt jwt, @RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		if (page < 0 || size < 1 || size > 100) {
			throw new org.springframework.web.server.ResponseStatusException(
					org.springframework.http.HttpStatus.BAD_REQUEST,
					"page must be >= 0 and size must be between 1 and 100");
		}
		return ResponseEntity.ok(service.viewOwnHistory(UUID.fromString(jwt.getSubject()),
				PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")))));
	}
}
