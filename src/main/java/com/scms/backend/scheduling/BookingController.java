package com.scms.backend.scheduling;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BookingController {

	private final BookingService service;

	BookingController(BookingService service) {
		this.service = service;
	}

	@PostMapping("/members/class-sessions/{sessionId}/bookings")
	ResponseEntity<BookingResponse> book(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID sessionId) {
		return ResponseEntity.status(201).body(service.book(UUID.fromString(jwt.getSubject()), sessionId));
	}

	@PatchMapping("/members/me/bookings/{bookingId}/cancel")
	ResponseEntity<BookingResponse> cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID bookingId) {
		return ResponseEntity.ok(service.cancel(UUID.fromString(jwt.getSubject()), bookingId));
	}
}
