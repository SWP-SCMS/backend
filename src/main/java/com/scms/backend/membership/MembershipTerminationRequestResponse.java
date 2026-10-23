package com.scms.backend.membership;

import java.time.Instant;
import java.util.UUID;

public record MembershipTerminationRequestResponse(UUID id, UUID membershipId, UUID memberAccountId,
		MembershipTerminationMode mode, MembershipTerminationRequestStatus status, String reason,
		UUID requesterAccountId, UUID approverAccountId, Instant effectiveAt, Long version, Instant createdAt,
		Instant updatedAt, String memberId, String memberFullName) {
	static MembershipTerminationRequestResponse from(MembershipTerminationRequest request) {
		return from(request, null, null);
	}
	static MembershipTerminationRequestResponse from(MembershipTerminationRequest request, String memberId,
			String memberFullName) {
		return new MembershipTerminationRequestResponse(request.getId(), request.getMembershipId(),
			request.getMemberAccountId(), request.getMode(), request.getStatus(), request.getReason(),
			request.getRequesterAccountId(), request.getApproverAccountId(), request.getEffectiveAt(),
			request.getVersion(), request.getCreatedAt(), request.getUpdatedAt(), memberId, memberFullName);
	}
}
