package com.scms.backend.scheduling.attendance;

import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/coach/class-sessions/{sessionId}/attendance")
class AttendanceController {

	final AttendanceService service;

	AttendanceController(AttendanceService s) {
		service = s;
	}

	@GetMapping
	ResponseEntity<List<AttendanceResponse>> list(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID sessionId) {
		return ResponseEntity.ok(service.list(UUID.fromString(jwt.getSubject()), sessionId));
	}

	@PatchMapping("/{attendanceId}")
	ResponseEntity<AttendanceResponse> update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID sessionId, @PathVariable UUID attendanceId, @RequestBody AttendanceUpdateRequest req) {
		return ResponseEntity.ok(service.update(UUID.fromString(jwt.getSubject()), sessionId, attendanceId, req));
	}
}
