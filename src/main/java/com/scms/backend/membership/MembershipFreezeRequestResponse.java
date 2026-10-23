package com.scms.backend.membership;

import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record MembershipFreezeRequestResponse(UUID id, UUID membershipId, UUID memberAccountId, LocalDate startDate,
		LocalDate endDate, String reason, BigInteger feeAmount, MembershipFreezeRequestStatus status,
		UUID requesterAccountId, UUID approverAccountId, Long version, Instant createdAt, Instant updatedAt,
		String memberId, String memberFullName) {
	static MembershipFreezeRequestResponse from(MembershipFreezeRequest request) {
		return from(request, null, null);
	}
	static MembershipFreezeRequestResponse from(MembershipFreezeRequest request, String memberId,
			String memberFullName) {
		return new MembershipFreezeRequestResponse(request.getId(), request.getMembershipId(), request.getMemberAccountId(),
			request.getStartDate(), request.getEndDate(), request.getReason(), request.getFeeAmount(), request.getStatus(),
			request.getRequesterAccountId(), request.getApproverAccountId(), request.getVersion(), request.getCreatedAt(), request.getUpdatedAt(),
			memberId, memberFullName);
	}
}
