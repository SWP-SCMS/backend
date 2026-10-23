package com.scms.backend.membership;

public enum MembershipFreezeRequestStatus {
	PENDING,
	APPROVED_PENDING_PAYMENT,
	SCHEDULED,
	ACTIVE,
	COMPLETED,
	OBSOLETE,
	REJECTED
}
