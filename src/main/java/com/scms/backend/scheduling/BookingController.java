package com.scms.backend.scheduling;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import jakarta.validation.Valid;

@RestController
public class BookingController {

	private final BookingService service;

	BookingController(BookingService service) {
		this.service = service;
	}

	@PostMapping("/members/class-sessions/{sessionId}/bookings")
	@ResponseStatus(HttpStatus.CREATED)
	ResponseEntity<BookingResponse> book(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID sessionId) {
		return ResponseEntity.status(201).body(service.book(UUID.fromString(jwt.getSubject()), sessionId));
	}

	@PatchMapping("/members/me/bookings/{bookingId}/cancel")
	ResponseEntity<BookingResponse> cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID bookingId) {
		return ResponseEntity.ok(service.cancel(UUID.fromString(jwt.getSubject()), bookingId));
	}

	@GetMapping("/members/me/bookings")
	ResponseEntity<BookingSchedulePageResponse> listForMember(@AuthenticationPrincipal Jwt jwt,
			@RequestParam(required = false) BookingStatus status,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
		return ResponseEntity.ok(service.listForMember(UUID.fromString(jwt.getSubject()), status,
			pageable(page, size)));
	}

	@PostMapping("/reception/members/{memberId}/bookings")
	@ResponseStatus(HttpStatus.CREATED)
	ResponseEntity<BookingResponse> bookForMember(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID memberId,
			@Valid @RequestBody ReceptionBookingRequest request) {
		return ResponseEntity.status(201).body(service.bookForMember(UUID.fromString(jwt.getSubject()), memberId,
			request.sessionId()));
	}

	@PatchMapping("/reception/members/{memberId}/bookings/{bookingId}/cancel")
	ResponseEntity<BookingResponse> cancelForMember(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID memberId,
			@PathVariable UUID bookingId) {
		return ResponseEntity.ok(service.cancelForMember(UUID.fromString(jwt.getSubject()), memberId, bookingId));
	}

	@GetMapping("/reception/members/{memberId}/bookings")
	ResponseEntity<BookingSchedulePageResponse> listForReceptionist(@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID memberId, @RequestParam(required = false) BookingStatus status,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
		return ResponseEntity.ok(service.listForReceptionist(UUID.fromString(jwt.getSubject()), memberId, status,
			pageable(page, size)));
	}

	private PageRequest pageable(int page, int size) {
		if (page < 0 || size < 1 || size > 100) {
			throw RecurringScheduleException.validation("page",
				"page must be >= 0 and size must be between 1 and 100");
		}
		return PageRequest.of(page, size, Sort.by("createdAt").descending().and(Sort.by("id")));
	}
}
