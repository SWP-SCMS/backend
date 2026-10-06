package com.scms.backend.scheduling;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/members/class-sessions")
public class MemberClassScheduleController {

	private final ClassSessionService service;

	MemberClassScheduleController(ClassSessionService service) {
		this.service = service;
	}

	@GetMapping
	ResponseEntity<MemberClassSessionPageResponse> list(@AuthenticationPrincipal Jwt jwt,
			@RequestParam(required = false) Instant from, @RequestParam(required = false) Instant to,
			@RequestParam(required = false) UUID classId, @RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		if (page < 0 || size < 1 || size > 100) {
			throw RecurringScheduleException.validation("page", "page must be >= 0 and size must be between 1 and 100");
		}
		var pageable = PageRequest.of(page, size,
			Sort.by("startTime").ascending().and(Sort.by("id")));
		return ResponseEntity.ok(service.memberSchedule(UUID.fromString(jwt.getSubject()), from, to, classId,
			pageable));
	}
}
