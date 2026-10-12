package com.scms.backend.membership;

import java.time.Instant;
import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/manager")
class ManagerReportingController {

	private final ManagerReportingService service;

	ManagerReportingController(ManagerReportingService service) {
		this.service = service;
	}

	@GetMapping("/dashboard")
	@Operation(description = "Requires MANAGER. Uses UTC ISO-8601 and the half-open [from,to) range.")
	ResponseEntity<ManagerDashboardResponse> dashboard(@AuthenticationPrincipal Jwt jwt,
			@RequestParam(required = false) @Parameter(example = "2026-10-07T17:00:00Z") Instant from,
			@RequestParam(required = false) @Parameter(example = "2026-10-08T17:00:00Z") Instant to,
			@RequestParam(defaultValue = "false") boolean includeTestData) {
		return ResponseEntity.ok(service.dashboard(id(jwt), from, to, includeTestData));
	}

	@GetMapping("/reports/members")
	ResponseEntity<MemberReportResponse> members(@AuthenticationPrincipal Jwt jwt,
			@RequestParam(required = false) Instant from, @RequestParam(required = false) Instant to,
			@RequestParam(defaultValue = "false") boolean includeTestData) {
		return ResponseEntity.ok(service.members(id(jwt), from, to, includeTestData));
	}

	@GetMapping("/reports/bookings")
	ResponseEntity<BookingReportResponse> bookings(@AuthenticationPrincipal Jwt jwt,
			@RequestParam(required = false) Instant from, @RequestParam(required = false) Instant to,
			@RequestParam(defaultValue = "false") boolean includeTestData) {
		return ResponseEntity.ok(service.bookings(id(jwt), from, to, includeTestData));
	}

	@GetMapping("/reports/attendance")
	ResponseEntity<AttendanceReportResponse> attendance(@AuthenticationPrincipal Jwt jwt,
			@RequestParam(required = false) Instant from, @RequestParam(required = false) Instant to,
			@RequestParam(defaultValue = "false") boolean includeTestData) {
		return ResponseEntity.ok(service.attendance(id(jwt), from, to, includeTestData));
	}

	@GetMapping("/reports/center-visits")
	ResponseEntity<CenterVisitReportResponse> centerVisits(@AuthenticationPrincipal Jwt jwt,
			@RequestParam(required = false) Instant from, @RequestParam(required = false) Instant to,
			@RequestParam(defaultValue = "false") boolean includeTestData) {
		return ResponseEntity.ok(service.centerVisits(id(jwt), from, to, includeTestData));
	}

	@GetMapping("/reports/class-utilization")
	ResponseEntity<ClassUtilizationReportResponse> classUtilization(@AuthenticationPrincipal Jwt jwt,
			@RequestParam(required = false) Instant from, @RequestParam(required = false) Instant to,
			@RequestParam(defaultValue = "false") boolean includeTestData) {
		return ResponseEntity.ok(service.classUtilization(id(jwt), from, to, includeTestData));
	}

	private UUID id(Jwt jwt) {
		return UUID.fromString(jwt.getSubject());
	}
}
