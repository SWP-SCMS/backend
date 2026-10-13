package com.scms.backend.training;

import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * US40 - Coach - Assigned Member List: GET /coach/members.
 * US41 - Coach - Member Training Profile: GET /coach/members/{memberId}.
 */
@RestController
@RequestMapping("/coach/members")
public class CoachAssignedMemberController {

	private final CoachAssignedMemberService service;

	public CoachAssignedMemberController(CoachAssignedMemberService service) {
		this.service = service;
	}

	/** US40: list all Members currently assigned to the authenticated Coach. */
	@GetMapping
	public ResponseEntity<CoachAssignedMemberPageResponse> list(@AuthenticationPrincipal Jwt jwt,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		if (page < 0 || size < 1 || size > 100) {
			throw new org.springframework.web.server.ResponseStatusException(
					org.springframework.http.HttpStatus.BAD_REQUEST,
					"page must be >= 0 and size must be between 1 and 100");
		}
		var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
		return ResponseEntity.ok(service.listAssignedMembers(coachId(jwt), pageable));
	}

	/** US41: get full training profile of an assigned Member. */
	@GetMapping("/{memberId}")
	public ResponseEntity<CoachMemberTrainingProfileResponse> getProfile(@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID memberId) {
		return ResponseEntity.ok(service.getMemberTrainingProfile(coachId(jwt), memberId));
	}

	private UUID coachId(Jwt jwt) {
		return UUID.fromString(jwt.getSubject());
	}
}
