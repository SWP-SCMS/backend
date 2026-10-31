package com.scms.backend.course;

import java.util.List;
import java.util.UUID;

import com.scms.backend.course.CourseTransferApiModels.ApproveTransfer;
import com.scms.backend.course.CourseTransferApiModels.RejectTransfer;
import com.scms.backend.course.CourseTransferApiModels.TransferOffer;
import com.scms.backend.course.CourseTransferApiModels.TransferRequest;
import com.scms.backend.course.CourseTransferApiModels.TransferTarget;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
class CourseTransferController {
	private final CourseTransferService service;
	CourseTransferController(CourseTransferService service) { this.service = service; }

	@GetMapping("/manager/course-transfer-requests")
	ResponseEntity<List<TransferRequest>> queue(@AuthenticationPrincipal Jwt jwt) {
		return ResponseEntity.ok(service.managerQueue(subject(jwt)));
	}
	@GetMapping("/manager/course-transfer-requests/{requestId}/targets")
	ResponseEntity<List<TransferTarget>> targets(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID requestId) {
		return ResponseEntity.ok(service.eligibleTargets(subject(jwt), requestId));
	}
	@PostMapping("/manager/course-transfer-requests/{requestId}/approve")
	ResponseEntity<TransferOffer> approve(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID requestId,
			@RequestBody ApproveTransfer request) {
		return ResponseEntity.ok(service.approve(subject(jwt), requestId, request == null ? null : request.targetRunId(),
			request == null ? null : request.reason()));
	}
	@PostMapping("/manager/course-transfer-requests/{requestId}/reject")
	ResponseEntity<TransferRequest> reject(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID requestId,
			@RequestBody RejectTransfer request) {
		return ResponseEntity.ok(service.reject(subject(jwt), requestId, request == null ? null : request.reason()));
	}
	@GetMapping("/members/me/course-transfer-offers")
	ResponseEntity<List<TransferOffer>> offers(@AuthenticationPrincipal Jwt jwt) {
		return ResponseEntity.ok(service.memberOffers(subject(jwt)));
	}
	@PostMapping("/members/me/course-transfer-offers/{offerId}/accept")
	ResponseEntity<TransferOffer> accept(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID offerId,
			@RequestHeader(value = "Idempotency-Key", required = false) String key) {
		return ResponseEntity.ok(service.accept(subject(jwt), offerId, key));
	}
	private static UUID subject(Jwt jwt) { return UUID.fromString(jwt.getSubject()); }
}
