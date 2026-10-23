package com.scms.backend.scheduling;

import java.time.Instant;
import java.util.UUID;

record WaitlistResponse(UUID id, UUID sessionId, UUID memberId, String memberCode, String memberFullName,
		String memberPhone, WaitlistStatus status, Integer position,
		String source, String terminalReasonCode, Instant createdAt, Instant terminalAt) {
	static WaitlistResponse from(SessionWaitlist entry, long ahead,
			com.scms.backend.account.AccountRepository.MemberIdentityView member) {
		return new WaitlistResponse(entry.getId(), entry.getSessionId(), entry.getMemberId(),
			member.getMemberCode(), member.getFullName(), member.getPhone(), entry.getStatus(),
			entry.getStatus() == WaitlistStatus.WAITING ? Math.toIntExact(ahead + 1) : null,
			entry.getSource(), entry.getTerminalReasonCode(), entry.getCreatedAt(), entry.getTerminalAt());
	}
}
