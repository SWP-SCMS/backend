package com.scms.backend.membership;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
class MembershipRefundController {
	private final MembershipRefundService service;
	MembershipRefundController(MembershipRefundService service) { this.service = service; }

	@GetMapping("/reception/refund-requests")
	ResponseEntity<RefundRequestPageResponse> receptionList(@AuthenticationPrincipal Jwt jwt,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
		return ResponseEntity.ok(service.listForReceptionist(id(jwt), page, size));
	}
	@GetMapping(value = "/reception/members/{memberCode}/refund-eligible-sources",
		produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<List<RefundEligibleSourceResponse>> refundEligibleSources(@AuthenticationPrincipal Jwt jwt,
			@PathVariable String memberCode) {
		return ResponseEntity.ok(service.refundEligibleSources(id(jwt), memberCode));
	}
	@PostMapping("/reception/members/{memberCode}/refund-requests")
	ResponseEntity<RefundRequestResponse> create(@AuthenticationPrincipal Jwt jwt, @PathVariable String memberCode,
			@RequestBody RefundRequestCreateRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(service.create(id(jwt), memberCode, request));
	}
	@PostMapping("/reception/refund-requests/{requestId}/cash-disbursement")
	ResponseEntity<RefundRequestResponse> cashDisbursement(@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID requestId, @RequestBody RefundCashDisbursementRequest request) {
		return ResponseEntity.ok(service.cashDisbursement(id(jwt), requestId, request));
	}
	@GetMapping("/manager/refund-requests")
	ResponseEntity<RefundRequestPageResponse> managerList(@AuthenticationPrincipal Jwt jwt,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
		return ResponseEntity.ok(service.listForManager(id(jwt), page, size));
	}
	@PostMapping("/manager/refund-requests/{requestId}/first-approval")
	ResponseEntity<RefundRequestResponse> firstApproval(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID requestId,
			@RequestBody RefundFirstApprovalRequest request) {
		return ResponseEntity.ok(service.firstApproval(id(jwt), requestId, request));
	}
	@PostMapping("/manager/refund-requests/{requestId}/final-approval")
	ResponseEntity<RefundRequestResponse> finalApproval(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID requestId,
			@RequestBody RefundActionRequest request) {
		return ResponseEntity.ok(service.finalApproval(id(jwt), requestId, request));
	}
	@PostMapping("/manager/refund-requests/{requestId}/reject")
	ResponseEntity<RefundRequestResponse> reject(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID requestId,
			@RequestBody RefundActionRequest request) {
		return ResponseEntity.ok(service.reject(id(jwt), requestId, request));
	}
	private UUID id(Jwt jwt) { return UUID.fromString(jwt.getSubject()); }
}
