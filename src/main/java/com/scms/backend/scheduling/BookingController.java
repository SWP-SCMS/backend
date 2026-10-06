package com.scms.backend.scheduling;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/members/class-sessions/{sessionId}/bookings")
public class BookingController {

	private final BookingService service;

	BookingController(BookingService service) {
		this.service = service;
	}

	@PostMapping
	ResponseEntity<BookingResponse> book(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID sessionId) {
		return ResponseEntity.status(201).body(service.book(UUID.fromString(jwt.getSubject()), sessionId));
	}
}
