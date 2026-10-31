package com.scms.backend.course;

import java.util.UUID;

import com.scms.backend.course.CourseRefundApiModels.RefundDecision;
import com.scms.backend.course.CourseRefundApiModels.RefundObligation;
import com.scms.backend.course.CourseRefundApiModels.RefundObligationPage;
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
class CourseRefundObligationController {
	private final CourseRefundObligationService service;
	CourseRefundObligationController(CourseRefundObligationService service) { this.service = service; }

	@GetMapping("/manager/course-refund-obligations")
	ResponseEntity<RefundObligationPage> list(@AuthenticationPrincipal Jwt jwt,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
			@RequestParam(required = false) String status) {
		return ResponseEntity.ok(service.list(subject(jwt), page, size, status));
	}
	@GetMapping("/manager/course-refund-obligations/{obligationId}")
	ResponseEntity<RefundObligation> detail(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID obligationId) {
		return ResponseEntity.ok(service.detail(subject(jwt), obligationId));
	}
	@PostMapping("/manager/course-refund-obligations/{obligationId}/approve")
	ResponseEntity<RefundObligation> approve(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID obligationId,
			@RequestBody RefundDecision request) {
		return ResponseEntity.ok(service.approve(subject(jwt), obligationId, version(request), reason(request)));
	}
	@PostMapping("/manager/course-refund-obligations/{obligationId}/reject")
	ResponseEntity<RefundObligation> reject(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID obligationId,
			@RequestBody RefundDecision request) {
		return ResponseEntity.ok(service.reject(subject(jwt), obligationId, version(request), reason(request)));
	}

	private static UUID subject(Jwt jwt) { return UUID.fromString(jwt.getSubject()); }
	private static Long version(RefundDecision request) { return request == null ? null : request.expectedVersion(); }
	private static String reason(RefundDecision request) { return request == null ? null : request.reason(); }
}
