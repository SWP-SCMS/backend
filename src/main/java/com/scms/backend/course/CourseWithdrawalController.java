package com.scms.backend.course;

import java.util.List;
import java.util.UUID;

import com.scms.backend.course.CourseWithdrawalApiModels.WithdrawalCreate;
import com.scms.backend.course.CourseWithdrawalApiModels.WithdrawalDecision;
import com.scms.backend.course.CourseWithdrawalApiModels.WithdrawalRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
class CourseWithdrawalController {
	private final CourseWithdrawalService service;
	CourseWithdrawalController(CourseWithdrawalService service) { this.service = service; }

	@PostMapping("/members/me/course-enrollments/{enrollmentId}/withdrawal-requests")
	ResponseEntity<WithdrawalRequest> memberCreate(@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID enrollmentId, @RequestBody WithdrawalCreate request) {
		return ResponseEntity.status(HttpStatus.CREATED)
			.body(service.createForMember(subject(jwt), enrollmentId, reason(request)));
	}
	@GetMapping("/members/me/course-withdrawal-requests")
	ResponseEntity<List<WithdrawalRequest>> memberList(@AuthenticationPrincipal Jwt jwt) {
		return ResponseEntity.ok(service.listForMember(subject(jwt)));
	}
	@GetMapping("/members/me/course-withdrawal-requests/{requestId}")
	ResponseEntity<WithdrawalRequest> memberDetail(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID requestId) {
		return ResponseEntity.ok(service.detailForMember(subject(jwt), requestId));
	}
	@PostMapping("/reception/members/{memberId}/course-enrollments/{enrollmentId}/withdrawal-requests")
	ResponseEntity<WithdrawalRequest> receptionCreate(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID memberId,
			@PathVariable UUID enrollmentId, @RequestBody WithdrawalCreate request) {
		return ResponseEntity.status(HttpStatus.CREATED)
			.body(service.createForReceptionist(subject(jwt), memberId, enrollmentId, reason(request)));
	}
	@GetMapping("/manager/course-withdrawal-requests")
	ResponseEntity<List<WithdrawalRequest>> managerQueue(@AuthenticationPrincipal Jwt jwt) {
		return ResponseEntity.ok(service.managerQueue(subject(jwt)));
	}
	@GetMapping("/manager/course-withdrawal-requests/{requestId}")
	ResponseEntity<WithdrawalRequest> managerDetail(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID requestId) {
		return ResponseEntity.ok(service.managerDetail(subject(jwt), requestId));
	}
	@PostMapping("/manager/course-withdrawal-requests/{requestId}/approve")
	ResponseEntity<WithdrawalRequest> approve(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID requestId,
			@RequestBody WithdrawalDecision request) {
		return ResponseEntity.ok(service.approve(subject(jwt), requestId, decisionReason(request)));
	}
	@PostMapping("/manager/course-withdrawal-requests/{requestId}/reject")
	ResponseEntity<WithdrawalRequest> reject(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID requestId,
			@RequestBody WithdrawalDecision request) {
		return ResponseEntity.ok(service.reject(subject(jwt), requestId, decisionReason(request)));
	}
	@PostMapping("/manager/members/{memberId}/course-enrollments/{enrollmentId}/withdrawal-requests")
	ResponseEntity<WithdrawalRequest> managerCreate(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID memberId,
			@PathVariable UUID enrollmentId, @RequestBody WithdrawalCreate request) {
		return ResponseEntity.status(HttpStatus.CREATED)
			.body(service.createForManager(subject(jwt), memberId, enrollmentId, reason(request)));
	}

	private static UUID subject(Jwt jwt) { return UUID.fromString(jwt.getSubject()); }
	private static String reason(WithdrawalCreate request) { return request == null ? null : request.reason(); }
	private static String decisionReason(WithdrawalDecision request) { return request == null ? null : request.reason(); }
}
