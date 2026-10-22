package com.scms.backend.scheduling;

import java.time.Instant;
import java.util.UUID;

import com.scms.backend.account.AccountRole;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
class PtSessionController {
	private final ClassSessionService service;
	PtSessionController(ClassSessionService service) { this.service = service; }

	@GetMapping("/members/pt-sessions")
	ResponseEntity<MemberClassSessionPageResponse> member(@AuthenticationPrincipal Jwt jwt,
			@RequestParam(required = false) Instant from, @RequestParam(required = false) Instant to,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
		return ResponseEntity.ok(service.memberPtSchedule(subject(jwt), from, to, page(page, size)));
	}

	@GetMapping("/reception/pt-sessions")
	ResponseEntity<MemberClassSessionPageResponse> reception(@AuthenticationPrincipal Jwt jwt,
			@RequestParam(required = false) Instant from, @RequestParam(required = false) Instant to,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
		return ResponseEntity.ok(service.staffPtSchedule(subject(jwt), AccountRole.RECEPTIONIST, from, to, page(page, size)));
	}

	@GetMapping("/manager/pt-sessions")
	ResponseEntity<MemberClassSessionPageResponse> manager(@AuthenticationPrincipal Jwt jwt,
			@RequestParam(required = false) Instant from, @RequestParam(required = false) Instant to,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
		return ResponseEntity.ok(service.staffPtSchedule(subject(jwt), AccountRole.MANAGER, from, to, page(page, size)));
	}

	@GetMapping("/coach/pt-sessions")
	ResponseEntity<MemberClassSessionPageResponse> coach(@AuthenticationPrincipal Jwt jwt,
			@RequestParam(required = false) Instant from, @RequestParam(required = false) Instant to,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
		return ResponseEntity.ok(service.staffPtSchedule(subject(jwt), AccountRole.COACH, from, to, page(page, size)));
	}

	private PageRequest page(int page, int size) {
		if (page < 0 || size < 1 || size > 100) throw RecurringScheduleException.validation("page",
			"page must be >= 0 and size must be between 1 and 100");
		return PageRequest.of(page, size, Sort.by("startTime").ascending().and(Sort.by("id")));
	}
	private UUID subject(Jwt jwt) { return UUID.fromString(jwt.getSubject()); }
}
