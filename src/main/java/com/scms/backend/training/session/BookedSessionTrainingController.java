package com.scms.backend.training.session;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BookedSessionTrainingController {

	private final BookedSessionTrainingService service;

	public BookedSessionTrainingController(BookedSessionTrainingService service) {
		this.service = service;
	}

	@GetMapping("/coach/pt-sessions/{sessionId}/members/{memberId}/training-summary")
	public BookedSessionTrainingService.Summary pt(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID sessionId, @PathVariable UUID memberId) {
		return service.get(UUID.fromString(jwt.getSubject()), sessionId, memberId, false);
	}

	@PutMapping("/coach/pt-sessions/{sessionId}/members/{memberId}/training-summary")
	public BookedSessionTrainingService.Summary savePt(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID sessionId, @PathVariable UUID memberId, @Valid @RequestBody Request request) {
		return service.save(UUID.fromString(jwt.getSubject()), sessionId, memberId, false, request.trainingPlanVersionId(), request.notes(), request.feedback());
	}

	@GetMapping("/coach/course-sessions/{sessionId}/members/{memberId}/training-summary")
	public BookedSessionTrainingService.Summary course(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID sessionId, @PathVariable UUID memberId) {
		return service.get(UUID.fromString(jwt.getSubject()), sessionId, memberId, true);
	}

	@PutMapping("/coach/course-sessions/{sessionId}/members/{memberId}/training-summary")
	public BookedSessionTrainingService.Summary saveCourse(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID sessionId, @PathVariable UUID memberId, @Valid @RequestBody Request request) {
		return service.save(UUID.fromString(jwt.getSubject()), sessionId, memberId, true, request.trainingPlanVersionId(), request.notes(), request.feedback());
	}

	public record Request(UUID trainingPlanVersionId, @NotBlank @Size(max = 4000) String notes, @NotBlank @Size(max = 4000) String feedback) {
	}
}
