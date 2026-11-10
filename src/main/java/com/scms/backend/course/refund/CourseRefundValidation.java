package com.scms.backend.course.refund;

import static com.scms.backend.course.refund.CourseRefundApiModels.RefundObligation;
import com.scms.backend.course.CourseRefundException;
import com.scms.backend.course.CourseRefundStatus;
import java.math.BigInteger;
import java.util.Set;
import com.scms.backend.course.refund.CourseRefundObligationService.DisbursementInput;
import com.scms.backend.course.refund.CourseRefundObligationService.VerificationInput;

final class CourseRefundValidation {

	static String reason(String value) {
		if (value == null || value.isBlank() || value.trim().length() > 1000)
			throw CourseRefundException.invalid("A non-blank reason of at most 1000 characters is required.");
		return value.trim();
	}

	static String status(String value) {
		if (value == null || value.isBlank())
			return null;
		try {
			return CourseRefundStatus.valueOf(value.trim().toUpperCase(java.util.Locale.ROOT)).name();
		} catch (IllegalArgumentException exception) {
			throw CourseRefundException.invalid("Unknown refund status.");
		}
	}

	static void page(int page, int size) {
		if (page < 0 || size < 1 || size > 100)
			throw CourseRefundException.invalid("Invalid page or size.");
	}

	static String key(String value) {
		if (value == null || value.isBlank() || value.trim().length() > 100)
			throw CourseRefundException.invalid("Idempotency-Key is required and must be at most 100 characters.");
		return value.trim();
	}

	static DisbursementInput disbursement(CourseRefundApiModels.RefundDisbursement request) {
		if (request == null || request.expectedVersion() == null || request.expectedVersion() < 0 || !vnd(request.amountVnd()) || blank(request.method(), 32) || blank(request.reference(), 200) || blank(request.evidenceReference(), 500)) {
			throw CourseRefundException.invalid("Valid version, VND amount, method, reference, and evidence are required.");
		}
		String method = request.method().trim().toUpperCase(java.util.Locale.ROOT);
		if (!Set.of("BANK_TRANSFER", "CASH").contains(method))
			throw CourseRefundException.invalid("Disbursement method must be BANK_TRANSFER or CASH.");
		return new DisbursementInput(request.expectedVersion(), request.amountVnd(), method, request.reference().trim(), request.evidenceReference().trim());
	}

	static VerificationInput verification(CourseRefundApiModels.RefundVerification request) {
		if (request == null || request.expectedVersion() == null || request.expectedVersion() < 0 || !vnd(request.amountVnd()) || blank(request.disbursementReference(), 200) || blank(request.evidenceReference(), 500)) {
			throw CourseRefundException.invalid("Valid version, amount, disbursement reference, and evidence are required.");
		}
		return new VerificationInput(request.expectedVersion(), request.amountVnd(), request.disbursementReference().trim(), request.evidenceReference().trim(), reason(request.reason()));
	}

	static boolean vnd(BigInteger value) {
		return value != null && value.signum() >= 0 && value.toString().length() <= 19;
	}

	static boolean blank(String value, int maximum) {
		return value == null || value.isBlank() || value.trim().length() > maximum;
	}

	static boolean matches(RefundObligation obligation, DisbursementInput input) {
		return input.amount().equals(obligation.disbursedAmountVnd()) && input.method().equals(obligation.disbursementMethod()) && input.reference().equals(obligation.disbursementReference()) && input.evidence().equals(obligation.evidenceReference());
	}

	static CourseRefundException idempotencyConflict() {
		return CourseRefundException.conflict("COURSE_REFUND_IDEMPOTENCY_CONFLICT", "Idempotency-Key was already used with another disbursement request.");
	}

	static CourseRefundException transition() {
		return CourseRefundException.conflict("COURSE_REFUND_INVALID_TRANSITION", "The refund obligation is not in a valid state for this action.");
	}

	static void version(RefundObligation obligation, Long expectedVersion) {
		if (expectedVersion == null || expectedVersion < 0)
			throw CourseRefundException.invalid("expectedVersion is required.");
		if (obligation.version() != expectedVersion)
			throw changed();
	}

	static CourseRefundException changed() {
		return CourseRefundException.conflict("COURSE_REFUND_CHANGED", "The refund obligation changed. Refresh and retry.");
	}

	static CourseRefundException alreadyDecided() {
		return CourseRefundException.conflict("COURSE_REFUND_ALREADY_DECIDED", "The refund obligation already has another decision.");
	}
}
