package com.scms.backend.scheduling;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/manager/class-sessions")
public class ClassSessionController {

	private final ClassSessionService service;

	ClassSessionController(ClassSessionService service) {
		this.service = service;
	}

	@PostMapping
	ResponseEntity<ClassSessionResponse> create(@AuthenticationPrincipal Jwt jwt,
			@RequestBody ClassSessionCreateRequest request) {
		return ResponseEntity.status(201).body(service.create(UUID.fromString(jwt.getSubject()), request));
	}
}
