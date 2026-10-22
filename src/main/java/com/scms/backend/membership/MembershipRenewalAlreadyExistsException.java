package com.scms.backend.membership;

public class MembershipRenewalAlreadyExistsException extends RuntimeException {
	public MembershipRenewalAlreadyExistsException() {
		super("The member already has a reserved membership or pending renewal order.");
	}
}
