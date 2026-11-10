package com.scms.backend.course.enrollment;

import com.scms.backend.course.order.CourseOrderResponse;
import com.scms.backend.course.refund.CourseRefundApiModels.MemberRefundObligation;
import com.scms.backend.course.refund.CourseRefundApiModels.MemberRefundObligationPage;
import com.scms.backend.course.refund.CourseRefundObligationService;
import com.scms.backend.course.schedulechange.CourseScheduleChangeApiModels.ScheduleChange;
import com.scms.backend.course.schedulechange.CourseScheduleChangeApiModels.ScheduleChangeResponse;
import com.scms.backend.course.schedulechange.CourseScheduleChangeService;
import com.scms.backend.course.transfer.CourseTransferApiModels.TransferOffer;
import com.scms.backend.course.transfer.CourseTransferService;
import com.scms.backend.course.waitlist.CourseWaitlistResponse;
import com.scms.backend.course.waitlist.CourseWaitlistService;
import com.scms.backend.course.withdrawal.CourseWithdrawalApiModels.WithdrawalRequest;
import com.scms.backend.course.withdrawal.CourseWithdrawalService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/reception/members/{memberId}", produces = MediaType.APPLICATION_JSON_VALUE)
class CourseReceptionActionsController {

	private final CourseWaitlistService waitlists;

	private final CourseScheduleChangeService changes;

	private final CourseTransferService transfers;

	private final CourseWithdrawalService withdrawals;

	private final CourseRefundObligationService refunds;

	CourseReceptionActionsController(CourseWaitlistService waitlists, CourseScheduleChangeService changes, CourseTransferService transfers, CourseWithdrawalService withdrawals, CourseRefundObligationService refunds) {
		this.waitlists = waitlists;
		this.changes = changes;
		this.transfers = transfers;
		this.withdrawals = withdrawals;
		this.refunds = refunds;
	}

	@GetMapping("/course-waitlists")
	ResponseEntity<List<CourseWaitlistResponse>> waitlists(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID memberId) {
		return ResponseEntity.ok(waitlists.mineForReceptionist(subject(jwt), memberId));
	}

	@PostMapping("/course-runs/{runId}/waitlist")
	ResponseEntity<CourseWaitlistResponse> join(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID memberId, @PathVariable UUID runId) {
		return ResponseEntity.status(201).body(waitlists.join(subject(jwt), memberId, runId));
	}

	@DeleteMapping("/course-runs/{runId}/waitlist")
	ResponseEntity<CourseWaitlistResponse> leave(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID memberId, @PathVariable UUID runId) {
		return ResponseEntity.ok(waitlists.leave(subject(jwt), memberId, runId));
	}

	@PostMapping("/course-waitlists/{waitlistId}/accept")
	ResponseEntity<CourseOrderResponse> acceptWaitlist(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID memberId, @PathVariable UUID waitlistId, @Parameter(required = true, schema = @Schema(minLength = 1, maxLength = 100)) @Size(min = 1, max = 100) @RequestHeader(name = "Idempotency-Key", required = false) String key) {
		var result = waitlists.accept(subject(jwt), memberId, waitlistId, key);
		return ResponseEntity.status(result.created() ? 201 : 200).body(result.response());
	}

	@GetMapping("/course-schedule-changes")
	ResponseEntity<List<ScheduleChange>> changes(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID memberId) {
		return ResponseEntity.ok(changes.listForReceptionist(subject(jwt), memberId));
	}

	@PostMapping("/course-schedule-changes/{responseId}/response")
	ResponseEntity<ScheduleChange> respond(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID memberId, @PathVariable UUID responseId, @RequestBody ScheduleChangeResponse request) {
		return ResponseEntity.ok(changes.respond(subject(jwt), memberId, responseId, request == null ? null : request.response()));
	}

	@GetMapping("/course-transfer-offers")
	ResponseEntity<List<TransferOffer>> offers(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID memberId) {
		return ResponseEntity.ok(transfers.offersForReceptionist(subject(jwt), memberId));
	}

	@PostMapping("/course-transfer-offers/{offerId}/accept")
	ResponseEntity<TransferOffer> acceptTransfer(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID memberId, @PathVariable UUID offerId, @Parameter(required = true, schema = @Schema(minLength = 1, maxLength = 100)) @Size(min = 1, max = 100) @RequestHeader(name = "Idempotency-Key", required = false) String key) {
		return ResponseEntity.ok(transfers.accept(subject(jwt), memberId, offerId, key));
	}

	@GetMapping("/course-withdrawal-requests")
	ResponseEntity<List<WithdrawalRequest>> withdrawals(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID memberId) {
		return ResponseEntity.ok(withdrawals.listForReceptionist(subject(jwt), memberId));
	}

	@GetMapping("/course-withdrawal-requests/{requestId}")
	ResponseEntity<WithdrawalRequest> withdrawal(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID memberId, @PathVariable UUID requestId) {
		return ResponseEntity.ok(withdrawals.detailForReceptionist(subject(jwt), memberId, requestId));
	}

	@GetMapping("/course-refund-obligations")
	ResponseEntity<MemberRefundObligationPage> refunds(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID memberId, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
		return ResponseEntity.ok(refunds.memberHistoryForReceptionist(subject(jwt), memberId, page, size));
	}

	@GetMapping("/course-refund-obligations/{obligationId}")
	ResponseEntity<MemberRefundObligation> refund(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID memberId, @PathVariable UUID obligationId) {
		return ResponseEntity.ok(refunds.memberDetailForReceptionist(subject(jwt), memberId, obligationId));
	}

	private static UUID subject(Jwt jwt) {
		return UUID.fromString(jwt.getSubject());
	}
}
