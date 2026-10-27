package com.scms.backend.scheduling;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/manager/coaches")
class CoachAvailabilityController {

	private final ClassSessionService service;

	CoachAvailabilityController(ClassSessionService service) {
		this.service = service;
	}

	@GetMapping("/available")
	ResponseEntity<List<AvailableCoachResponse>> available(@AuthenticationPrincipal Jwt jwt,
			@RequestParam List<Instant> startTime, @RequestParam List<Instant> endTime,
			@RequestParam(required = false) UUID memberAccountId) {
		return ResponseEntity.ok(service.availableCoaches(UUID.fromString(jwt.getSubject()), startTime, endTime,
			memberAccountId));
	}
}
