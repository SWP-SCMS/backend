package com.scms.backend.reception;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
class CenterVisitController {

	private final CenterVisitService service;

	CenterVisitController(CenterVisitService service) {
		this.service = service;
	}

	@PostMapping("/reception/center-visits")
	ResponseEntity<CenterVisitResponse> checkIn(@AuthenticationPrincipal Jwt jwt,
			@RequestBody CenterVisitRequest request) {
		return ResponseEntity.status(201).body(service.checkIn(UUID.fromString(jwt.getSubject()), request));
	}
}
