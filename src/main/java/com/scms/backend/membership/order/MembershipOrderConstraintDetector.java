package com.scms.backend.membership.order;

import org.hibernate.exception.ConstraintViolationException;

final public class MembershipOrderConstraintDetector {

	static final String PENDING_MEMBER_CONSTRAINT = "uq_membership_orders_pending_member";

	private MembershipOrderConstraintDetector() {
	}

	public static boolean isPendingMemberConstraint(Throwable failure) {
		for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
			if (cause instanceof ConstraintViolationException constraintViolation && PENDING_MEMBER_CONSTRAINT.equals(constraintViolation.getConstraintName())) {
				return true;
			}
			String message = cause.getMessage();
			if (message != null && message.contains(PENDING_MEMBER_CONSTRAINT)) {
				return true;
			}
		}
		return false;
	}
}
