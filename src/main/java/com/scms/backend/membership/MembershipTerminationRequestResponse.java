package com.scms.backend.membership;

import java.time.Instant;
import java.util.UUID;

public record MembershipTerminationRequestResponse(UUID id, UUID membershipId, UUID memberAccountId,
		String memberId, String memberFullName,
		MembershipTerminationMode mode, MembershipTerminationRequestStatus status, String reason,
		UUID requesterAccountId, UUID approverAccountId, Instant effectiveAt, Long version, Instant createdAt,
		Instant updatedAt) {
	static MembershipTerminationRequestResponse from(MembershipTerminationRequest request) {
		return from(request, null);
	}
	static MembershipTerminationRequestResponse from(MembershipTerminationRequest request,
			com.scms.backend.account.AccountRepository.MemberIdentityView member) {
		return new MembershipTerminationRequestResponse(request.getId(), request.getMembershipId(),
			request.getMemberAccountId(), member == null ? null : member.getMemberCode(),
			member == null ? null : member.getFullName(), request.getMode(), request.getStatus(), request.getReason(),
			request.getRequesterAccountId(), request.getApproverAccountId(), request.getEffectiveAt(),
			request.getVersion(), request.getCreatedAt(), request.getUpdatedAt());
	}
}
