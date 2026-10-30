package com.scms.backend.membership;

import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "OK",
			content = @Content(schema = @Schema(implementation = SepayPaymentResponse.class))),
		@ApiResponse(responseCode = "503", description = "SePay destination configuration is invalid",
			content = @Content(mediaType = "application/problem+json",
				schema = @Schema(ref = "#/components/schemas/ApiProblem")))
	})
	ResponseEntity<SepayPaymentResponse> create(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID orderId) {
		return ResponseEntity.ok(service.create(UUID.fromString(jwt.getSubject()), orderId));
	}

	@PostMapping("/reception/members/{memberId}/membership-orders/{orderId}/payments/sepay")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "OK",
			content = @Content(schema = @Schema(implementation = SepayPaymentResponse.class))),
		@ApiResponse(responseCode = "503", description = "SePay destination configuration is invalid",
			content = @Content(mediaType = "application/problem+json",
				schema = @Schema(ref = "#/components/schemas/ApiProblem")))
	})
	ResponseEntity<SepayPaymentResponse> createForReceptionist(@AuthenticationPrincipal Jwt jwt,
			@PathVariable String memberId, @PathVariable UUID orderId) {
		return ResponseEntity.ok(service.createForReceptionist(UUID.fromString(jwt.getSubject()), memberId, orderId));
	}

	@PostMapping("/payments/sepay/webhook")
	@SecurityRequirements
	@Operation(summary = "Receive a SePay transfer webhook", description = """
		Public provider callback authenticated by an API key in the Authorization header using `Apikey <credential>`.
		A 200 response with `success=true` acknowledges durable handling and does not necessarily mean the Payment is PAID.
		When `processed=false`, SCMS creates no Membership or Receipt and the event requires manual review.
		Valid CoursePayment evidence is recorded with `processed=false` and does not create an Enrollment in this task.
		""")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "Webhook durably handled or idempotently replayed"),
		@ApiResponse(responseCode = "400", description = "Malformed webhook request",
			content = @Content(mediaType = "application/problem+json",
				schema = @Schema(ref = "#/components/schemas/ApiProblem"))),
		@ApiResponse(responseCode = "401", description = "Invalid webhook API-key credentials",
			content = @Content(mediaType = "application/problem+json",
				schema = @Schema(ref = "#/components/schemas/ApiProblem"))),
		@ApiResponse(responseCode = "500", description = "Transient processing or storage failure; provider may retry",
			content = @Content(mediaType = "application/problem+json",
				schema = @Schema(ref = "#/components/schemas/ApiProblem")))
	})
	ResponseEntity<SepayWebhookResponse> webhook(
			@Parameter(description = "SePay API-key header in the form `Apikey <credential>`", required = true)
			@RequestHeader(name = "Authorization", required = false) String authorization,
			@Valid @RequestBody SepayWebhookRequest request) {
		String apiKey = authorization != null && authorization.startsWith("Apikey ")
			? authorization.substring(7) : authorization;
		return ResponseEntity.ok(service.webhook(apiKey, request));
	}
}
