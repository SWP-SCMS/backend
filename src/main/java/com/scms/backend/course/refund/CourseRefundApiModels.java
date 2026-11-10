package com.scms.backend.course.refund;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigInteger;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

final public class CourseRefundApiModels {

	private CourseRefundApiModels() {
	}

	record RefundDecision(Long expectedVersion, String reason) {
	}

	public record RefundDisbursement(Long expectedVersion, BigInteger amountVnd, String method, String reference, String evidenceReference) {
	}

	public record RefundVerification(Long expectedVersion, BigInteger amountVnd, String disbursementReference, String evidenceReference, String reason) {
	}

	record RefundTerminal(Long expectedVersion, String reason) {
	}

	public record RefundObligation(UUID id, UUID enrollmentId, UUID courseRunId, UUID memberAccountId, String memberCode, String memberName, String courseCode, String courseName, String runCode, String courseRunStatus, UUID courseOrderId, UUID coursePaymentId, String source, String status, BigInteger grossObligationVnd, BigInteger approvedAmountVnd, BigInteger disbursedAmountVnd, BigInteger verifiedAmountVnd, String currencyCode, UUID requesterAccountId, String requestReason, UUID approvedByAccountId, String approvalReason, Instant approvedAt, UUID rejectedByAccountId, String rejectionReason, Instant rejectedAt, UUID disbursedByAccountId, Instant disbursedAt, String disbursementMethod, String disbursementReference, String evidenceReference, UUID verifiedByAccountId, String verificationReason, Instant verifiedAt, UUID terminalByAccountId, String terminalReason, Instant terminalAt, Instant createdAt, Instant updatedAt, long version) {
	}

	record RefundObligationPage(List<RefundObligation> items, int page, int size, long totalElements, int totalPages) {
	}

	public record MemberRefundObligation(UUID id, UUID enrollmentId, UUID courseRunId, String source, String status, BigInteger grossObligationVnd, BigInteger approvedAmountVnd, BigInteger disbursedAmountVnd, BigInteger verifiedAmountVnd, String currencyCode, Instant createdAt, Instant updatedAt, long version) {
	}

	public record MemberRefundObligationPage(List<MemberRefundObligation> items, int page, int size, long totalElements, int totalPages) {
	}

	@Schema(name = "ReceptionCourseRefund", additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
	record ReceptionCourseRefund(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID refundId, @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID memberId, @Schema(requiredMode = Schema.RequiredMode.REQUIRED, types = { "string", "null" }) String memberCode, @Schema(requiredMode = Schema.RequiredMode.REQUIRED, types = { "string", "null" }) String memberName, @Schema(requiredMode = Schema.RequiredMode.REQUIRED, types = { "string", "null" }) UUID courseId, @Schema(requiredMode = Schema.RequiredMode.REQUIRED, types = { "string", "null" }) String courseCode, @Schema(requiredMode = Schema.RequiredMode.REQUIRED, types = { "string", "null" }) String courseName, @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID courseRunId, @Schema(requiredMode = Schema.RequiredMode.REQUIRED, types = { "string", "null" }) String runCode, @Schema(requiredMode = Schema.RequiredMode.REQUIRED, types = { "string", "null" }) String courseRunStatus, @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String source, @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0", maximum = "9999999999999999999") BigInteger amount, @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0") long version, @Schema(requiredMode = Schema.RequiredMode.REQUIRED, types = { "string", "null" }) Instant approvedAt, @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean disbursementEligible, @Schema(requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = { "ELIGIBLE", "AWAITING_COURSE_COMPLETION", "COURSE_RUN_NOT_FOUND" }) String disbursementEligibilityReason) {
	}

	@Schema(name = "ReceptionCourseRefundPage", additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
	record ReceptionCourseRefundPage(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<ReceptionCourseRefund> items, @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0") int page, @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "1", maximum = "100") int size, @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0") long totalElements, @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0") int totalPages) {
	}
}
