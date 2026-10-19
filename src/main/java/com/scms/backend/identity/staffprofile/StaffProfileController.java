package com.scms.backend.identity.staffprofile;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/staff/me/profile")
public class StaffProfileController {
	private final StaffProfileService service;

	StaffProfileController(StaffProfileService service) { this.service = service; }

	@GetMapping
	ResponseEntity<StaffProfileResponse> get(@AuthenticationPrincipal Jwt jwt) {
		return ResponseEntity.ok(service.getProfile(id(jwt)));
	}

	@PatchMapping
	ResponseEntity<StaffProfileResponse> update(@AuthenticationPrincipal Jwt jwt,
			@RequestBody StaffProfilePatchRequest request) {
		if (request == null) throw new IllegalArgumentException("request must be a JSON object");
		return ResponseEntity.ok(service.updateProfile(id(jwt), request));
	}

	private UUID id(Jwt jwt) { return UUID.fromString(jwt.getSubject()); }
}
