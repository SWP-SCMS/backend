package com.scms.backend.training;

import java.util.Optional;
import java.util.UUID;

import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/manager/members/{memberId}")
public class ManagerCoachAssignmentController {

	private final CoachAssignmentService service;

	public ManagerCoachAssignmentController(CoachAssignmentService service) {
		this.service = service;
	}

	@PostMapping("/coach-assignment")
	@ResponseStatus(HttpStatus.CREATED)
	public ResponseEntity<CoachAssignmentResponse> assign(@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID memberId, @Valid @RequestBody CoachAssignmentAssignRequest request) {
		CoachAssignmentResponse response = service.assign(accountId(jwt), memberId, request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@GetMapping("/coach-assignment")
	public ResponseEntity<CoachAssignmentResponse> getActive(@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID memberId) {
		Optional<CoachAssignmentResponse> response = service.getActive(accountId(jwt), memberId);
		return response.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
	}

	@GetMapping("/coach-assignments")
	public ResponseEntity<CoachAssignmentPageResponse> listHistory(@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID memberId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		if (page < 0 || size < 1 || size > 100) {
			throw CoachAssignmentException.validationError("page",
					"page must be >= 0 and size must be between 1 and 100");
		}
		Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
		return ResponseEntity.ok(service.listHistory(accountId(jwt), memberId, pageable));
	}

	@PatchMapping("/coach-assignment/end")
	public ResponseEntity<CoachAssignmentResponse> end(@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID memberId, @Valid @RequestBody CoachAssignmentEndRequest request) {
		return ResponseEntity.ok(service.endAssignment(accountId(jwt), memberId, request));
	}

	private UUID accountId(Jwt jwt) {
		return UUID.fromString(jwt.getSubject());
	}
}
