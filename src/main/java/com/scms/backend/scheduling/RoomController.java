package com.scms.backend.scheduling;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/manager/rooms")
public class RoomController {

	private final RoomService service;
	private final RoomSuspensionService suspensionService;

	RoomController(RoomService service, RoomSuspensionService suspensionService) {
		this.service = service;
		this.suspensionService = suspensionService;
	}

	@GetMapping
	ResponseEntity<List<RoomResponse>> list(@AuthenticationPrincipal Jwt jwt) {
		return ResponseEntity.ok(service.list(accountId(jwt)));
	}

	@GetMapping("/availability")
	ResponseEntity<List<RoomAvailabilityResponse>> availability(@AuthenticationPrincipal Jwt jwt,
			@RequestParam Instant startTime, @RequestParam Instant endTime, @RequestParam Integer requestedCapacity,
			@RequestParam(required = false) UUID excludeSessionId) {
		return ResponseEntity.ok(service.availability(accountId(jwt), startTime, endTime, requestedCapacity,
			excludeSessionId));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	ResponseEntity<RoomResponse> create(@AuthenticationPrincipal Jwt jwt, @RequestBody RoomCreateRequest request) {
		return ResponseEntity.status(201).body(service.create(accountId(jwt), request));
	}

	@GetMapping("/{roomId}")
	ResponseEntity<RoomResponse> get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID roomId) {
		return ResponseEntity.ok(service.get(accountId(jwt), roomId));
	}

	@PatchMapping("/{roomId}")
	ResponseEntity<RoomResponse> update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID roomId,
			@RequestBody RoomPatchRequest request) {
		return ResponseEntity.ok(service.update(accountId(jwt), roomId, request));
	}

	@GetMapping("/{roomId}/suspensions")
	ResponseEntity<List<RoomSuspensionResponse>> listSuspensions(@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID roomId) {
		return ResponseEntity.ok(suspensionService.list(accountId(jwt), roomId));
	}

	@PostMapping("/{roomId}/suspensions")
	@ResponseStatus(HttpStatus.CREATED)
	ResponseEntity<RoomSuspensionResponse> suspend(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID roomId,
			@RequestBody RoomSuspensionCreateRequest request) {
		return ResponseEntity.status(201).body(suspensionService.create(accountId(jwt), roomId, request));
	}

	@PatchMapping("/{roomId}/suspensions/{suspensionId}")
	ResponseEntity<RoomSuspensionResponse> extendSuspension(@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID roomId, @PathVariable UUID suspensionId,
			@Valid @RequestBody RoomSuspensionExtensionRequest request) {
		return ResponseEntity.ok(suspensionService.extend(accountId(jwt), roomId, suspensionId, request));
	}

	@PostMapping("/{roomId}/impact-preview")
	ResponseEntity<RoomImpactPreviewResponse> impactPreview(@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID roomId, @RequestBody RoomImpactPreviewRequest request) {
		return ResponseEntity.ok(suspensionService.previewImpact(accountId(jwt), roomId, request));
	}

	@PostMapping("/{roomId}/impact-apply")
	ResponseEntity<RoomResponse> impactApply(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID roomId,
			@RequestBody RoomImpactPreviewRequest request) {
		return ResponseEntity.ok(suspensionService.applyImpact(accountId(jwt), roomId, request));
	}

	private UUID accountId(Jwt jwt) {
		return UUID.fromString(jwt.getSubject());
	}
}
