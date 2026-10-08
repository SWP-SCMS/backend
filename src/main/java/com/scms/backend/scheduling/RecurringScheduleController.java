package com.scms.backend.scheduling;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/manager/recurring-schedules")
public class RecurringScheduleController {

	private final RecurringScheduleService service;

	RecurringScheduleController(RecurringScheduleService service) {
		this.service = service;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	ResponseEntity<RecurringScheduleResponse> create(@AuthenticationPrincipal Jwt jwt,
			@RequestBody RecurringScheduleCreateRequest request) {
		return ResponseEntity.status(201).body(service.create(UUID.fromString(jwt.getSubject()), request));
	}
}
