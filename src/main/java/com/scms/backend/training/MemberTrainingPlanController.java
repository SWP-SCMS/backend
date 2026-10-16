package com.scms.backend.training;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** US44 - Member - View own Training Plans and immutable versions. */
@RestController
@RequestMapping("/members/me/training-plans")
public class MemberTrainingPlanController {

	private final MemberTrainingPlanService service;

	public MemberTrainingPlanController(MemberTrainingPlanService service) {
		this.service = service;
	}

	@GetMapping
	public ResponseEntity<List<MemberTrainingPlanResponse>> viewOwnPlans(@AuthenticationPrincipal Jwt jwt) {
		return ResponseEntity.ok(service.viewOwnPlans(UUID.fromString(jwt.getSubject())));
	}
}
