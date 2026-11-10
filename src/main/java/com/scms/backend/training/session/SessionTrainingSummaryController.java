package com.scms.backend.training.session;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/coach/class-sessions/{sessionId}/members/{memberId}/training-summary")
public class SessionTrainingSummaryController {

	private final SessionTrainingSummaryService service;

	public SessionTrainingSummaryController(SessionTrainingSummaryService service) {
		this.service = service;
	}

	@PutMapping
	public SessionTrainingSummaryService.Summary save(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID sessionId, @PathVariable UUID memberId, @Valid @RequestBody Request request) {
		return service.save(UUID.fromString(jwt.getSubject()), sessionId, memberId, request.notes(), request.feedback());
	}

	public record Request(@NotBlank String notes, @NotBlank String feedback) {
	}
}
