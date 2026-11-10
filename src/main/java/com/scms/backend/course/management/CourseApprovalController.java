package com.scms.backend.course.management;

import com.scms.backend.course.CourseException;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
class CourseApprovalController {

	private final CourseManagementService service;

	CourseApprovalController(CourseManagementService service) {
		this.service = service;
	}

	@GetMapping("/manager/course-approval-requests")
	ResponseEntity<CourseApprovalPageResponse> queue(@AuthenticationPrincipal Jwt jwt, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
		return ResponseEntity.ok(service.listCourseApprovals(accountId(jwt), page(page, size)));
	}

	@GetMapping("/manager/course-approval-requests/{courseId}")
	ResponseEntity<CourseApprovalResponse> detail(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID courseId) {
		return ResponseEntity.ok(service.getCourseApproval(accountId(jwt), courseId));
	}

	@PostMapping("/manager/course-approval-requests/{courseId}/approve")
	ResponseEntity<CourseApprovalResponse> approve(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID courseId, @Valid @RequestBody CourseApprovalDecision request) {
		return ResponseEntity.ok(service.approveCourse(accountId(jwt), courseId, request));
	}

	@PostMapping("/manager/course-approval-requests/{courseId}/reject")
	ResponseEntity<CourseApprovalResponse> reject(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID courseId, @Valid @RequestBody CourseApprovalDecision request) {
		return ResponseEntity.ok(service.rejectCourse(accountId(jwt), courseId, request));
	}

	private PageRequest page(int page, int size) {
		if (page < 0)
			throw CourseException.validation("page", "must be greater than or equal to zero");
		if (size < 1 || size > 100)
			throw CourseException.validation("size", "must be between 1 and 100");
		return PageRequest.of(page, size, Sort.by("submittedAt").ascending().and(Sort.by("id")));
	}

	private UUID accountId(Jwt jwt) {
		return UUID.fromString(jwt.getSubject());
	}
}
