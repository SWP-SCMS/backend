package com.scms.backend.scheduling;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/manager/rooms")
public class RoomController {

	private final RoomService service;

	RoomController(RoomService service) {
		this.service = service;
	}

	@GetMapping
	ResponseEntity<List<RoomResponse>> list(@AuthenticationPrincipal Jwt jwt) {
		return ResponseEntity.ok(service.list(accountId(jwt)));
	}

	@PostMapping
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

	private UUID accountId(Jwt jwt) {
		return UUID.fromString(jwt.getSubject());
	}
}
