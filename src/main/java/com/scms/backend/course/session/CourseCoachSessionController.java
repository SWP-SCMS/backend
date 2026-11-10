package com.scms.backend.course.session;

import com.scms.backend.course.session.CourseCoachSessionApiModels.CourseCoachSessionPage;
import com.scms.backend.course.session.CourseCoachSessionApiModels.CourseCoachSessionResponse;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
class CourseCoachSessionController {

	private final CourseCoachSessionService service;

	CourseCoachSessionController(CourseCoachSessionService service) {
		this.service = service;
	}

	@GetMapping("/coach/course-sessions")
	ResponseEntity<CourseCoachSessionPage> list(@AuthenticationPrincipal Jwt jwt, @RequestParam(required = false) Instant from, @RequestParam(required = false) Instant to, @RequestParam(required = false) String status, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size, @RequestParam(required = false) List<String> sort) {
		return ResponseEntity.ok(service.list(subject(jwt), from, to, status, page, size, sort));
	}

	@GetMapping("/coach/course-sessions/{sessionId}")
	ResponseEntity<CourseCoachSessionResponse> detail(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID sessionId) {
		return ResponseEntity.ok(service.detail(subject(jwt), sessionId));
	}

	private static UUID subject(Jwt jwt) {
		return UUID.fromString(jwt.getSubject());
	}
}
