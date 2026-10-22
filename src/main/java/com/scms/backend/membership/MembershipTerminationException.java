package com.scms.backend.membership;

import org.springframework.http.HttpStatus;

public class MembershipTerminationException extends RuntimeException {
	private final HttpStatus status;
	private final String code;

	MembershipTerminationException(HttpStatus status, String code, String message) {
		super(message);
		this.status = status;
		this.code = code;
	}

	static MembershipTerminationException notActive() { return conflict("MEMBERSHIP_NOT_ACTIVE", "The membership is not active."); }
	static MembershipTerminationException alreadyPending() { return conflict("MEMBERSHIP_TERMINATION_ALREADY_PENDING", "A termination request is already pending."); }
	static MembershipTerminationException notFound() { return new MembershipTerminationException(HttpStatus.NOT_FOUND, "MEMBERSHIP_TERMINATION_NOT_FOUND", "The termination request was not found."); }
	static MembershipTerminationException invalidTransition() { return conflict("MEMBERSHIP_TERMINATION_INVALID_TRANSITION", "The termination request cannot transition from its current state."); }
	static MembershipTerminationException changed() { return conflict("MEMBERSHIP_TERMINATION_CHANGED", "The termination request was changed by another operation."); }
	private static MembershipTerminationException conflict(String code, String message) { return new MembershipTerminationException(HttpStatus.CONFLICT, code, message); }
	public HttpStatus getStatus() { return status; }
	public String getCode() { return code; }
}
