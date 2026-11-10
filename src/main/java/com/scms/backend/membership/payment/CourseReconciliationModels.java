package com.scms.backend.membership.payment;

import com.scms.backend.course.enrollment.CourseEnrollmentResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigInteger;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class CourseReconciliationModels {

	private CourseReconciliationModels() {
	}

	@Schema(name = "CourseReconciliationResult")
	public record Result(UUID paymentId, UUID orderId, UUID reservationId, UUID courseRunId, UUID memberId, String orderReference, BigInteger amount, String currency, String status, String orderStatus, String reservationStatus, String method, Instant deadlineAt, CourseEnrollmentResponse enrollment) {
	}

	@Schema(name = "CourseReconciliationDetail")
	public record Detail(Result payment, String bankCode, String bankAccountNumber, String bankAccountName, String transferContent, boolean canConfirmPaid, boolean canConfirmFailed, String blockedReason) {
	}

	@Schema(name = "CourseReconciliationQueueItem")
	public record QueueItem(UUID paymentId, UUID orderId, UUID reservationId, UUID courseRunId, UUID memberId, String memberCode, String memberName, String orderReference, String paymentReference, BigInteger amount, String currency, String status, Instant deadlineAt, Instant createdAt) {
	}

	@Schema(name = "CourseReconciliationPage")
	public record Page(List<QueueItem> content, int page, int size, long totalElements, int totalPages) {
	}
}
