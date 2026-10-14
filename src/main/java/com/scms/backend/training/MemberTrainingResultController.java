package com.scms.backend.training;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** US47 - Member - View Training Result. */
@RestController
@RequestMapping("/members/me/training-results")
public class MemberTrainingResultController {

	private final MemberTrainingResultService service;

	public MemberTrainingResultController(MemberTrainingResultService service) {
		this.service = service;
	}

	@GetMapping
	public ResponseEntity<List<MemberTrainingResultResponse>> viewOwnHistory(@AuthenticationPrincipal Jwt jwt) {
		return ResponseEntity.ok(service.viewOwnHistory(UUID.fromString(jwt.getSubject())));
	}
}
