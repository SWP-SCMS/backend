package com.scms.backend.notification;

import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

	private final NotificationService service;

	NotificationController(NotificationService service) {
		this.service = service;
	}

	@GetMapping
	@Operation(summary = "List the authenticated account's notifications")
	ResponseEntity<NotificationPageResponse> list(@AuthenticationPrincipal Jwt jwt,
			@RequestParam(defaultValue = "false") boolean unreadOnly,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
		return ResponseEntity.ok(service.list(accountId(jwt), unreadOnly, pageable(page, size)));
	}

	@PatchMapping("/{notificationId}/read")
	@Operation(summary = "Mark an owned notification as read")
	ResponseEntity<NotificationReadResponse> markRead(@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID notificationId) {
		return ResponseEntity.ok(service.markRead(accountId(jwt), notificationId));
	}

	@PatchMapping("/read-all")
	@Operation(summary = "Mark all notifications as read")
	ResponseEntity<NotificationReadResponse> markAllRead(@AuthenticationPrincipal Jwt jwt) {
		return ResponseEntity.ok(service.markAllRead(accountId(jwt)));
	}

	private Pageable pageable(int page, int size) {
		if (page < 0) {
			throw new NotificationValidationException("page", "must be >= 0");
		}
		if (size < 1 || size > 100) {
			throw new NotificationValidationException("size", "must be between 1 and 100");
		}
		return PageRequest.of(page, size, Sort.by("createdAt").descending().and(Sort.by("id").descending()));
	}

	private UUID accountId(Jwt jwt) {
		return UUID.fromString(jwt.getSubject());
	}
}
