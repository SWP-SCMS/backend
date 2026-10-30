package com.scms.backend.course;

import java.util.UUID;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
class CourseOrderController {

	private final CourseOrderService service;

	CourseOrderController(CourseOrderService service) {
		this.service = service;
	}

	@PostMapping("/members/me/course-orders")
	ResponseEntity<CourseOrderResponse> createForMember(@AuthenticationPrincipal Jwt jwt,
			@Parameter(required = true)
			@RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
			@Valid @RequestBody CourseOrderCreateRequest request) {
		return createdOrReplayed(service.createForMember(accountId(jwt), request, idempotencyKey));
	}

	@GetMapping("/members/me/course-orders/{orderId}")
	ResponseEntity<CourseOrderResponse> getForMember(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID orderId) {
		return ResponseEntity.ok(service.getForMember(accountId(jwt), orderId));
	}

	@PostMapping("/reception/members/{memberId}/course-orders")
	ResponseEntity<CourseOrderResponse> createForReceptionist(@AuthenticationPrincipal Jwt jwt,
			@PathVariable String memberId,
			@Parameter(required = true)
			@RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
			@Valid @RequestBody CourseOrderCreateRequest request) {
		return createdOrReplayed(service.createForReceptionist(accountId(jwt), memberId, request, idempotencyKey));
	}

	@GetMapping("/reception/members/{memberId}/course-orders/{orderId}")
	ResponseEntity<CourseOrderResponse> getForReceptionist(@AuthenticationPrincipal Jwt jwt,
			@PathVariable String memberId, @PathVariable UUID orderId) {
		return ResponseEntity.ok(service.getForReceptionist(accountId(jwt), memberId, orderId));
	}

	private ResponseEntity<CourseOrderResponse> createdOrReplayed(CourseOrderService.CreateResult result) {
		return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK).body(result.response());
	}

	private UUID accountId(Jwt jwt) {
		return UUID.fromString(jwt.getSubject());
	}
}
