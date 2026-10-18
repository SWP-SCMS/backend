package com.scms.backend.training;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/members/me/coach-assignment")
public class MemberCoachAssignmentController {

	private final MemberCoachAssignmentService service;

	public MemberCoachAssignmentController(MemberCoachAssignmentService service) {
		this.service = service;
	}

	@GetMapping
	public ResponseEntity<MemberCoachAssignmentResponse> getActive(@AuthenticationPrincipal Jwt jwt) {
		return service.getActive(UUID.fromString(jwt.getSubject()))
				.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
	}
}
