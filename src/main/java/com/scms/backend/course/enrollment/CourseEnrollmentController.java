package com.scms.backend.course.enrollment;

import com.scms.backend.course.enrollment.CourseEnrollmentApiModels.AttendanceUpdate;
import com.scms.backend.course.enrollment.CourseEnrollmentApiModels.MemberEnrollment;
import com.scms.backend.course.enrollment.CourseEnrollmentApiModels.ReceptionCourseEnrollment;
import com.scms.backend.course.enrollment.CourseEnrollmentApiModels.ReceptionCourseEnrollmentPage;
import com.scms.backend.course.enrollment.CourseEnrollmentApiModels.RosterEntry;
import com.scms.backend.course.schedulechange.CourseScheduleChangeApiModels;
import com.scms.backend.course.schedulechange.CourseScheduleChangeApiModels.ScheduleChange;
import com.scms.backend.course.schedulechange.CourseScheduleChangeApiModels.ScheduleChangeResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
class CourseEnrollmentController {

	private final CourseEnrollmentService service;

	private final ReceptionCourseEnrollmentService receptionService;

	CourseEnrollmentController(CourseEnrollmentService service, ReceptionCourseEnrollmentService receptionService) {
		this.service = service;
		this.receptionService = receptionService;
	}

	@GetMapping("/reception/members/{memberId}/course-enrollments")
	ResponseEntity<ReceptionCourseEnrollmentPage> receptionEnrollments(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID memberId, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size, @RequestParam(required = false) String status, @RequestParam(required = false) Boolean withdrawalEligible) {
		return ResponseEntity.ok(receptionService.list(subject(jwt), memberId, page, size, status, withdrawalEligible));
	}

	@GetMapping("/reception/members/{memberId}/course-enrollments/{enrollmentId}")
	ResponseEntity<ReceptionCourseEnrollment> receptionEnrollment(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID memberId, @PathVariable UUID enrollmentId) {
		return ResponseEntity.ok(receptionService.detail(subject(jwt), memberId, enrollmentId));
	}

	private static UUID subject(Jwt jwt) {
		return UUID.fromString(jwt.getSubject());
	}

	@GetMapping("/members/me/course-enrollments")
	ResponseEntity<List<MemberEnrollment>> memberEnrollments(@AuthenticationPrincipal Jwt jwt) {
		return ResponseEntity.ok(service.memberEnrollments(UUID.fromString(jwt.getSubject())));
	}

	@GetMapping("/members/me/course-schedule-changes")
	ResponseEntity<List<ScheduleChange>> scheduleChanges(@AuthenticationPrincipal Jwt jwt) {
		return ResponseEntity.ok(service.scheduleChanges(UUID.fromString(jwt.getSubject())));
	}

	@PostMapping("/members/me/course-schedule-changes/{responseId}/response")
	ResponseEntity<ScheduleChange> respondToScheduleChange(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID responseId, @RequestBody ScheduleChangeResponse request) {
		return ResponseEntity.ok(service.respondToScheduleChange(UUID.fromString(jwt.getSubject()), responseId, request));
	}

	@RestController
	@RequestMapping("/coach/course-sessions/{sessionId}/roster")
	static class CoachRosterController {

		private final CourseEnrollmentService service;

		CoachRosterController(CourseEnrollmentService service) {
			this.service = service;
		}

		@GetMapping
		ResponseEntity<List<RosterEntry>> roster(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID sessionId) {
			return ResponseEntity.ok(service.roster(UUID.fromString(jwt.getSubject()), sessionId));
		}

		@io.swagger.v3.oas.annotations.Operation(description = "Assigned active Coach only. Requires ACTIVE enrollment, Run CONFIRMED/IN_PROGRESS/COMPLETED and an effective session within [startAt,endAt+30 minutes). Inactive Run or closed window returns 409 COURSE_ATTENDANCE_WINDOW_CLOSED; cancelled Run or wrong assignment returns 404.")
		@PatchMapping("/{enrollmentSessionId}/attendance")
		ResponseEntity<RosterEntry> attendance(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID sessionId, @PathVariable UUID enrollmentSessionId, @RequestBody AttendanceUpdate request) {
			return ResponseEntity.ok(service.updateAttendance(UUID.fromString(jwt.getSubject()), sessionId, enrollmentSessionId, request));
		}
	}
}
