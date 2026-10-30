package com.scms.backend.course;

import java.util.UUID;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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

	@PostMapping(value = "/members/me/course-orders", consumes = MediaType.APPLICATION_JSON_VALUE,
		produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<CourseOrderResponse> createForMember(@AuthenticationPrincipal Jwt jwt,
			@Parameter(required = true, schema = @Schema(minLength = 1, maxLength = 100))
			@Size(min = 1, max = 100)
			@RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
			@Valid @RequestBody CourseOrderCreateRequest request) {
		return createdOrReplayed(service.createForMember(accountId(jwt), request, idempotencyKey));
	}

	@GetMapping(value = "/members/me/course-orders/{orderId}", produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<CourseOrderResponse> getForMember(@AuthenticationPrincipal Jwt jwt,
			@Parameter(required = true, schema = @Schema(format = "uuid")) @PathVariable UUID orderId) {
		return ResponseEntity.ok(service.getForMember(accountId(jwt), orderId));
	}

	@PostMapping(value = "/reception/members/{memberId}/course-orders", consumes = MediaType.APPLICATION_JSON_VALUE,
		produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<CourseOrderResponse> createForReceptionist(@AuthenticationPrincipal Jwt jwt,
			@Parameter(required = true, schema = @Schema(format = "uuid")) @PathVariable UUID memberId,
			@Parameter(required = true, schema = @Schema(minLength = 1, maxLength = 100))
			@Size(min = 1, max = 100)
			@RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
			@Valid @RequestBody CourseOrderCreateRequest request) {
		return createdOrReplayed(service.createForReceptionist(accountId(jwt), memberId, request, idempotencyKey));
	}

	@GetMapping(value = "/reception/members/{memberId}/course-orders/{orderId}",
		produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<CourseOrderResponse> getForReceptionist(@AuthenticationPrincipal Jwt jwt,
			@Parameter(required = true, schema = @Schema(format = "uuid")) @PathVariable UUID memberId,
			@Parameter(required = true, schema = @Schema(format = "uuid")) @PathVariable UUID orderId) {
		return ResponseEntity.ok(service.getForReceptionist(accountId(jwt), memberId, orderId));
	}

	private ResponseEntity<CourseOrderResponse> createdOrReplayed(CourseOrderService.CreateResult result) {
		return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK).body(result.response());
	}

	private UUID accountId(Jwt jwt) {
		return UUID.fromString(jwt.getSubject());
	}
}
