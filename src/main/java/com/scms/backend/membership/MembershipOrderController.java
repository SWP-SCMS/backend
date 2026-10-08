package com.scms.backend.membership;

import java.util.Optional;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseStatus;

@RestController
public class MembershipOrderController {

	private final MembershipOrderService service;

	MembershipOrderController(MembershipOrderService service) {
		this.service = service;
	}

	@PostMapping("/members/me/membership-orders")
	@ResponseStatus(HttpStatus.CREATED)
	ResponseEntity<MembershipOrderResponse> createForMember(@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody MembershipOrderCreateRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(service.createForMember(accountId(jwt), request));
	}

	@GetMapping("/members/me/membership-orders/pending")
	ResponseEntity<MembershipOrderResponse> resumeForMember(@AuthenticationPrincipal Jwt jwt) {
		return optionalResponse(service.resumeForMember(accountId(jwt)));
	}

	@PostMapping("/reception/members/{memberId}/membership-orders")
	@ResponseStatus(HttpStatus.CREATED)
	ResponseEntity<MembershipOrderResponse> createForReceptionist(@AuthenticationPrincipal Jwt jwt,
			@PathVariable String memberId,
			@Valid @RequestBody MembershipOrderCreateRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED)
			.body(service.createForReceptionist(accountId(jwt), memberId, request));
	}

	@GetMapping("/reception/members/{memberId}/membership-orders/pending")
	ResponseEntity<MembershipOrderResponse> resumeForReceptionist(@AuthenticationPrincipal Jwt jwt,
			@PathVariable String memberId) {
		return optionalResponse(service.resumeForReceptionist(accountId(jwt), memberId));
	}

	private UUID accountId(Jwt jwt) {
		return UUID.fromString(jwt.getSubject());
	}

	private ResponseEntity<MembershipOrderResponse> optionalResponse(
			Optional<MembershipOrderResponse> response) {
		return response.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
	}
}
