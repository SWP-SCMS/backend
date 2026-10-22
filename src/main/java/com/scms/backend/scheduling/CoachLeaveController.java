package com.scms.backend.scheduling;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
class CoachLeaveController {
	private final CoachLeaveService service;
	CoachLeaveController(CoachLeaveService service) { this.service = service; }

	@PostMapping("/coach/leave-requests")
	ResponseEntity<CoachLeaveResponse> create(@AuthenticationPrincipal Jwt jwt, @RequestBody CoachLeaveCreateRequest request) {
		return ResponseEntity.status(201).body(service.create(subject(jwt), request));
	}
	@GetMapping("/coach/me/leave-requests")
	ResponseEntity<List<CoachLeaveResponse>> mine(@AuthenticationPrincipal Jwt jwt) {
		return ResponseEntity.ok(service.mine(subject(jwt)));
	}
	@DeleteMapping("/coach/leave-requests/{requestId}")
	ResponseEntity<CoachLeaveResponse> withdraw(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID requestId) {
		return ResponseEntity.ok(service.withdraw(subject(jwt), requestId));
	}
	@GetMapping("/manager/coach-leave-requests")
	ResponseEntity<List<CoachLeaveResponse>> list(@AuthenticationPrincipal Jwt jwt,
			@RequestParam(required = false) CoachLeaveStatus status) {
		return ResponseEntity.ok(service.managerList(subject(jwt), status));
	}
	@PatchMapping("/manager/coach-leave-requests/{requestId}/approve")
	ResponseEntity<CoachLeaveResponse> approve(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID requestId,
			@RequestBody(required = false) CoachLeaveReviewRequest request) {
		return ResponseEntity.ok(service.approve(subject(jwt), requestId, request));
	}
	@PatchMapping("/manager/coach-leave-requests/{requestId}/reject")
	ResponseEntity<CoachLeaveResponse> reject(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID requestId,
			@RequestBody(required = false) CoachLeaveReviewRequest request) {
		return ResponseEntity.ok(service.reject(subject(jwt), requestId, request));
	}
	private UUID subject(Jwt jwt) { return UUID.fromString(jwt.getSubject()); }
}
