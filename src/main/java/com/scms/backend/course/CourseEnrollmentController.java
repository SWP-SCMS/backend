package com.scms.backend.course;

import java.util.List;
import java.util.UUID;

import com.scms.backend.course.CourseEnrollmentApiModels.AttendanceUpdate;
import com.scms.backend.course.CourseEnrollmentApiModels.MemberEnrollment;
import com.scms.backend.course.CourseEnrollmentApiModels.RosterEntry;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class CourseEnrollmentController {
	private final CourseEnrollmentService service;
	CourseEnrollmentController(CourseEnrollmentService service) { this.service = service; }

	@GetMapping("/members/me/course-enrollments")
	ResponseEntity<List<MemberEnrollment>> memberEnrollments(@AuthenticationPrincipal Jwt jwt) {
		return ResponseEntity.ok(service.memberEnrollments(UUID.fromString(jwt.getSubject())));
	}

	@RestController
	@RequestMapping("/coach/course-sessions/{sessionId}/roster")
	static class CoachRosterController {
		private final CourseEnrollmentService service;
		CoachRosterController(CourseEnrollmentService service) { this.service = service; }
		@GetMapping ResponseEntity<List<RosterEntry>> roster(@AuthenticationPrincipal Jwt jwt,
				@PathVariable UUID sessionId) {
			return ResponseEntity.ok(service.roster(UUID.fromString(jwt.getSubject()), sessionId));
		}
		@PatchMapping("/{enrollmentSessionId}/attendance")
		ResponseEntity<RosterEntry> attendance(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID sessionId,
				@PathVariable UUID enrollmentSessionId, @RequestBody AttendanceUpdate request) {
			return ResponseEntity.ok(service.updateAttendance(UUID.fromString(jwt.getSubject()), sessionId,
				enrollmentSessionId, request));
		}
	}
}
