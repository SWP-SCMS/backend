package com.scms.backend.membership;

import java.util.UUID;

import io.swagger.v3.oas.annotations.security.SecurityRequirements;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
class SepayController {
	private final SepayService service;

	SepayController(SepayService service) { this.service = service; }

	@PostMapping({
		"/members/me/membership-orders/{orderId}/payments/sepay",
		"/members/me/membership-orders/{orderId}/bank-transfer"
	})
	ResponseEntity<SepayPaymentResponse> create(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID orderId) {
		return ResponseEntity.ok(service.create(UUID.fromString(jwt.getSubject()), orderId));
	}

	@PostMapping("/payments/sepay/webhook")
	@SecurityRequirements
	ResponseEntity<PaymentResultResponse> webhook(
			@RequestHeader(name = "Authorization", required = false) String authorization,
			@Valid @RequestBody SepayWebhookRequest request) {
		String apiKey = authorization != null && authorization.startsWith("Apikey ")
			? authorization.substring(7) : authorization;
		return ResponseEntity.ok(service.webhook(apiKey, request));
	}
}
