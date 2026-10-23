package com.scms.backend.membership;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
class MembershipFreezeController {
	private final MembershipFreezeService service;
	MembershipFreezeController(MembershipFreezeService service) { this.service = service; }

	@GetMapping("/members/me/membership-freeze-requests")
	ResponseEntity<MembershipFreezeRequestPageResponse> memberList(@AuthenticationPrincipal Jwt jwt,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
		return ResponseEntity.ok(service.listForMember(id(jwt), page, size(page, size)));
	}
	@PostMapping("/members/me/membership-freeze-requests")
	ResponseEntity<MembershipFreezeRequestResponse> memberCreate(@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody MembershipFreezeCreateRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(service.createForMember(id(jwt), request));
	}
	@PostMapping("/members/me/membership-freeze-requests/{requestId}/end-early")
	ResponseEntity<MembershipFreezeRequestResponse> memberEndEarly(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID requestId,
			@RequestBody MembershipFreezeDecisionRequest request) {
		return ResponseEntity.ok(service.endEarlyForMember(id(jwt), requestId, request));
	}
	@GetMapping("/reception/members/{memberCode}/membership-freeze-requests")
	ResponseEntity<MembershipFreezeRequestPageResponse> receptionList(@AuthenticationPrincipal Jwt jwt, @PathVariable String memberCode,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
		return ResponseEntity.ok(service.listForReceptionist(id(jwt), memberCode, page, size(page, size)));
	}
	@PostMapping("/reception/members/{memberCode}/membership-freeze-requests")
	ResponseEntity<MembershipFreezeRequestResponse> receptionCreate(@AuthenticationPrincipal Jwt jwt, @PathVariable String memberCode,
			@Valid @RequestBody MembershipFreezeCreateRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(service.createForReceptionist(id(jwt), memberCode, request));
	}
	@GetMapping("/manager/membership-freeze-requests")
	ResponseEntity<MembershipFreezeRequestPageResponse> managerList(@AuthenticationPrincipal Jwt jwt,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
		return ResponseEntity.ok(service.listForManager(id(jwt), page, size(page, size)));
	}
	@PostMapping("/manager/membership-freeze-requests/{requestId}/approve")
	ResponseEntity<MembershipFreezeRequestResponse> approve(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID requestId,
			@RequestBody MembershipFreezeDecisionRequest request) { return ResponseEntity.ok(service.approve(id(jwt), requestId, request)); }
	@PostMapping("/manager/membership-freeze-requests/{requestId}/reject")
	ResponseEntity<MembershipFreezeRequestResponse> reject(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID requestId,
			@RequestBody MembershipFreezeDecisionRequest request) { return ResponseEntity.ok(service.reject(id(jwt), requestId, request)); }
	@PostMapping("/reception/membership-freeze-requests/{requestId}/cash-payment")
	ResponseEntity<MembershipFreezeCashPaymentResponse> cash(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID requestId,
			@RequestBody MembershipFreezeDecisionRequest request) { return ResponseEntity.ok(service.collectCash(id(jwt), requestId, request)); }
	@PostMapping("/reception/membership-freeze-requests/{requestId}/end-early")
	ResponseEntity<MembershipFreezeRequestResponse> receptionEndEarly(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID requestId,
			@RequestBody MembershipFreezeDecisionRequest request) {
		return ResponseEntity.ok(service.endEarlyForReceptionist(id(jwt), requestId, request));
	}

	private UUID id(Jwt jwt) { return UUID.fromString(jwt.getSubject()); }
	private int size(int page, int size) { if (page < 0 || size < 1 || size > 100) throw new MembershipFreezeException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "page and size are invalid"); return size; }
}
