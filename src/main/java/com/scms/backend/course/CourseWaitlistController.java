package com.scms.backend.course;

import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
class CourseWaitlistController {
	private final CourseWaitlistService service;
	CourseWaitlistController(CourseWaitlistService service) { this.service = service; }

	@PostMapping(value = "/course-runs/{runId}/waitlist", produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<CourseWaitlistResponse> join(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID runId) {
		return ResponseEntity.status(201).body(service.join(subject(jwt), runId));
	}

	@DeleteMapping(value = "/course-runs/{runId}/waitlist", produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<CourseWaitlistResponse> leave(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID runId) {
		return ResponseEntity.ok(service.leave(subject(jwt), runId));
	}

	@GetMapping(value = "/members/me/course-waitlists", produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<List<CourseWaitlistResponse>> mine(@AuthenticationPrincipal Jwt jwt) {
		return ResponseEntity.ok(service.mine(subject(jwt)));
	}

	@PostMapping(value = "/course-waitlists/{waitlistId}/accept", produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<CourseOrderResponse> accept(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID waitlistId,
			@Parameter(required = true, schema = @Schema(minLength = 1, maxLength = 100))
			@Size(min = 1, max = 100)
			@RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey) {
		var result = service.accept(subject(jwt), waitlistId, idempotencyKey);
		return ResponseEntity.status(result.created() ? 201 : 200).body(result.response());
	}

	private UUID subject(Jwt jwt) { return UUID.fromString(jwt.getSubject()); }
}
