package com.scms.backend.membership;

import java.util.UUID;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
class MembershipTerminationController {
	private final MembershipTerminationService service;
	MembershipTerminationController(MembershipTerminationService service) { this.service = service; }

	@GetMapping("/members/me/membership-termination-requests")
	ResponseEntity<MembershipTerminationRequestPageResponse> memberList(@AuthenticationPrincipal Jwt jwt,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
		return ResponseEntity.ok(service.listForMember(id(jwt), page, validateSize(page, size)));
	}
	@PostMapping("/members/me/membership-termination-requests")
	ResponseEntity<MembershipTerminationRequestResponse> memberCreate(@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody MembershipTerminationCreateRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(service.createForMember(id(jwt), request));
	}
	@DeleteMapping("/members/me/membership-termination-requests/{requestId}")
	ResponseEntity<Void> memberWithdraw(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID requestId) {
		service.withdrawForMember(id(jwt), requestId); return ResponseEntity.noContent().build();
	}

	@GetMapping("/reception/members/{memberCode}/membership-termination-requests")
	ResponseEntity<MembershipTerminationRequestPageResponse> receptionList(@AuthenticationPrincipal Jwt jwt,
			@PathVariable String memberCode, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
		return ResponseEntity.ok(service.listForReceptionist(id(jwt), memberCode, page, validateSize(page, size)));
	}
	@PostMapping("/reception/members/{memberCode}/membership-termination-requests")
	ResponseEntity<MembershipTerminationRequestResponse> receptionCreate(@AuthenticationPrincipal Jwt jwt,
			@PathVariable String memberCode, @RequestBody MembershipTerminationCreateRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(service.createForReceptionist(id(jwt), memberCode, request));
	}

	@GetMapping("/manager/membership-termination-requests")
	ResponseEntity<MembershipTerminationRequestPageResponse> managerList(@AuthenticationPrincipal Jwt jwt,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
		return ResponseEntity.ok(service.listForManager(id(jwt), page, validateSize(page, size)));
	}
	@PostMapping("/manager/membership-termination-requests/{requestId}/approve")
	ResponseEntity<MembershipTerminationRequestResponse> approve(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID requestId,
			@RequestBody MembershipTerminationDecisionRequest request) { return ResponseEntity.ok(service.approve(id(jwt), requestId, request)); }
	@PostMapping("/manager/membership-termination-requests/{requestId}/reject")
	ResponseEntity<MembershipTerminationRequestResponse> reject(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID requestId,
			@RequestBody MembershipTerminationDecisionRequest request) { return ResponseEntity.ok(service.reject(id(jwt), requestId, request)); }
	@PostMapping("/manager/members/{memberAccountId}/membership-termination-requests")
	ResponseEntity<MembershipTerminationRequestResponse> direct(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID memberAccountId,
			@RequestBody MembershipTerminationCreateRequest request) { return ResponseEntity.status(HttpStatus.CREATED).body(service.createDirect(id(jwt), memberAccountId, request)); }

	private UUID id(Jwt jwt) { return UUID.fromString(jwt.getSubject()); }
	private int validateSize(int page, int size) { if (page < 0 || size < 1 || size > 100) throw new MembershipTerminationException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "page and size are invalid"); return size; }
}
