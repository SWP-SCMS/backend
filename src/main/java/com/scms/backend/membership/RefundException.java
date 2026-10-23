package com.scms.backend.membership;

import org.springframework.http.HttpStatus;

public class RefundException extends RuntimeException {
	private final HttpStatus status;
	private final String code;
	RefundException(HttpStatus status, String code, String message) { super(message); this.status = status; this.code = code; }
	static RefundException validation() { return value(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "The refund request is invalid."); }
	static RefundException notFound() { return value(HttpStatus.NOT_FOUND, "REFUND_NOT_FOUND", "The refund request was not found."); }
	static RefundException notEligible() { return value(HttpStatus.CONFLICT, "REFUND_NOT_ELIGIBLE", "The payment and membership are not eligible for refund."); }
	static RefundException exists() { return value(HttpStatus.CONFLICT, "REFUND_ALREADY_EXISTS", "An open refund request already exists for this payment."); }
	static RefundException amountLimit() { return value(HttpStatus.CONFLICT, "REFUND_AMOUNT_EXCEEDS_LIMIT", "The requested amount exceeds the eligible refund amount."); }
	static RefundException transition() { return value(HttpStatus.CONFLICT, "REFUND_INVALID_TRANSITION", "The refund request cannot transition from its current state."); }
	static RefundException changed() { return value(HttpStatus.CONFLICT, "REFUND_REQUEST_CHANGED", "The refund request changed during this operation."); }
	static RefundException unsupportedPayment() { return value(HttpStatus.CONFLICT, "REFUND_PAYMENT_METHOD_UNSUPPORTED", "Only paid CASH payments can be refunded."); }
	static RefundException threshold() { return value(HttpStatus.CONFLICT, "REFUND_REMAINING_DAYS_BELOW_THRESHOLD", "The remaining membership period is below the refund threshold."); }
	private static RefundException value(HttpStatus status, String code, String message) { return new RefundException(status, code, message); }
	public HttpStatus getStatus() { return status; }
	public String getCode() { return code; }
}
