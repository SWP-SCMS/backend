package com.scms.backend.membership;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
class PaymentController {
	private final PaymentService service;

	PaymentController(PaymentService service) { this.service = service; }

	@PostMapping("/reception/members/{memberId}/cash-payments")
	ResponseEntity<PaymentResultResponse> cash(@AuthenticationPrincipal Jwt jwt, @PathVariable String memberId,
			@RequestBody CashPaymentRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(service.cash(id(jwt), memberId, request));
	}

	@Deprecated
	@PostMapping("/reception/membership-orders/{orderId}/cash")
	ResponseEntity<Void> deprecatedCash(@PathVariable UUID orderId) {
		throw PaymentException.conflict("This endpoint is deprecated; confirm cash with /reception/members/{memberId}/cash-payments");
	}

	@PostMapping("/payments/{paymentId}/reconcile")
	ResponseEntity<PaymentResultResponse> reconcile(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID paymentId,
			@RequestBody PaymentActionRequest request) {
		return ResponseEntity.ok(service.reconcile(id(jwt), paymentId, request));
	}

	@PatchMapping("/membership-orders/{orderId}/cancel")
	ResponseEntity<MembershipOrderCancellationResponse> cancelOrder(@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID orderId, @RequestBody MembershipOrderCancellationRequest request) {
		return ResponseEntity.ok(service.cancelOrder(id(jwt), orderId, request));
	}

	@GetMapping("/payments/reconciliation-queue")
	ResponseEntity<ReconciliationQueuePageResponse> reconciliationQueue(@AuthenticationPrincipal Jwt jwt,
			@RequestParam(defaultValue = "ALL") ReconciliationQueueStatus status,
			@RequestParam(required = false) String query,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		if (page < 0 || size < 1 || size > 100) {
			throw PaymentException.validation("page must be >= 0 and size must be between 1 and 100");
		}
		return ResponseEntity.ok(service.reconciliationQueue(id(jwt), status, query, page, size));
	}

	@GetMapping("/members/me/receipts")
	ResponseEntity<List<ReceiptResponse>> receipts(@AuthenticationPrincipal Jwt jwt) {
		return ResponseEntity.ok(service.receipts(id(jwt)));
	}

	@GetMapping("/reception/members/{memberId}/receipts")
	ResponseEntity<List<ReceiptResponse>> receiptsForReceptionist(@AuthenticationPrincipal Jwt jwt,
			@PathVariable String memberId) {
		return ResponseEntity.ok(service.receiptsForReceptionist(id(jwt), memberId));
	}

	@GetMapping("/receipts/{receiptId}")
	ResponseEntity<ReceiptResponse> receipt(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID receiptId) {
		return ResponseEntity.ok(service.receipt(id(jwt), receiptId));
	}

	@GetMapping("/payments/{paymentId}/result")
	ResponseEntity<PaymentResultResponse> result(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID paymentId) {
		return ResponseEntity.ok(service.result(id(jwt), paymentId));
	}

	@GetMapping("/members/me/memberships")
	ResponseEntity<List<Map<String, Object>>> history(@AuthenticationPrincipal Jwt jwt) {
		return ResponseEntity.ok(service.memberHistory(id(jwt)));
	}

	@GetMapping("/manager/reports/membership-revenue")
	ResponseEntity<RevenueReportResponse> report(@AuthenticationPrincipal Jwt jwt,
			@RequestParam(required = false) Instant from, @RequestParam(required = false) Instant to,
			@RequestParam(defaultValue = "false") boolean includeTestData) {
		return ResponseEntity.ok(service.report(id(jwt), from, to, includeTestData));
	}

	private UUID id(Jwt jwt) { return UUID.fromString(jwt.getSubject()); }
}
