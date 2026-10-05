package com.scms.backend.scheduling;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
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
@RequestMapping("/manager/disciplines")
public class DisciplineController {

	private final DisciplineService service;

	DisciplineController(DisciplineService service) {
		this.service = service;
	}

	@GetMapping
	ResponseEntity<List<DisciplineResponse>> list(@AuthenticationPrincipal Jwt jwt) {
		return ResponseEntity.ok(service.list(accountId(jwt)));
	}

	@PostMapping
	ResponseEntity<DisciplineResponse> create(@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody DisciplineCreateRequest request) {
		return ResponseEntity.status(201).body(service.create(accountId(jwt), request));
	}

	@GetMapping("/{disciplineId}")
	ResponseEntity<DisciplineResponse> get(@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID disciplineId) {
		return ResponseEntity.ok(service.get(accountId(jwt), disciplineId));
	}

	@PatchMapping("/{disciplineId}")
	ResponseEntity<DisciplineResponse> update(@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID disciplineId, @RequestBody DisciplinePatchRequest request) {
		return ResponseEntity.ok(service.update(accountId(jwt), disciplineId, request));
	}

	private UUID accountId(Jwt jwt) {
		return UUID.fromString(jwt.getSubject());
	}
}
