package com.scms.backend.training.session;

import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MemberSessionTrainingController {

	private final MemberSessionTrainingService service;

	public MemberSessionTrainingController(MemberSessionTrainingService service) {
		this.service = service;
	}

	@GetMapping("/members/me/class-sessions/{sessionId}/training-summary")
	public MemberSessionTrainingService.Summary classSession(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID sessionId) {
		return service.get(UUID.fromString(jwt.getSubject()), sessionId, false);
	}

	@GetMapping("/members/me/course-sessions/{sessionId}/training-summary")
	public MemberSessionTrainingService.Summary courseSession(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID sessionId) {
		return service.get(UUID.fromString(jwt.getSubject()), sessionId, true);
	}
}
