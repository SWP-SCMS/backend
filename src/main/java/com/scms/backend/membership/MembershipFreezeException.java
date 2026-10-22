package com.scms.backend.membership;

import org.springframework.http.HttpStatus;

public class MembershipFreezeException extends RuntimeException {
	private final HttpStatus status; private final String code;
	MembershipFreezeException(HttpStatus status, String code, String message) { super(message); this.status = status; this.code = code; }
	static MembershipFreezeException notFound() { return value(HttpStatus.NOT_FOUND, "MEMBERSHIP_FREEZE_NOT_FOUND", "The membership freeze request was not found."); }
	static MembershipFreezeException alreadyPending() { return conflict("MEMBERSHIP_FREEZE_ALREADY_PENDING", "A membership freeze request is already open."); }
	static MembershipFreezeException invalidPeriod() { return value(HttpStatus.BAD_REQUEST, "MEMBERSHIP_FREEZE_INVALID_PERIOD", "The membership freeze period is invalid."); }
	static MembershipFreezeException validation() { return value(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "The membership freeze request is invalid."); }
	static MembershipFreezeException paymentRequired() { return conflict("MEMBERSHIP_FREEZE_PAYMENT_REQUIRED", "The membership freeze request is not approved for payment."); }
	static MembershipFreezeException invalidTransition() { return conflict("MEMBERSHIP_FREEZE_INVALID_TRANSITION", "The membership freeze request cannot transition from its current state."); }
	static MembershipFreezeException changed() { return conflict("MEMBERSHIP_FREEZE_CHANGED", "The membership freeze request was changed by another operation."); }
	static MembershipFreezeException notActive() { return conflict("MEMBERSHIP_NOT_ACTIVE", "The membership is not active."); }
	private static MembershipFreezeException conflict(String code, String message) { return value(HttpStatus.CONFLICT, code, message); }
	private static MembershipFreezeException value(HttpStatus status, String code, String message) { return new MembershipFreezeException(status, code, message); }
	public HttpStatus getStatus() { return status; } public String getCode() { return code; }
}
