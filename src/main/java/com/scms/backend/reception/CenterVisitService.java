package com.scms.backend.reception;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import com.scms.backend.account.AccountStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class CenterVisitService {

	private final ReceptionMemberProfileService members;
	private final CenterVisitRepository visits;
	private final Clock clock;

	CenterVisitService(ReceptionMemberProfileService members, CenterVisitRepository visits, Clock clock) {
		this.members = members;
		this.visits = visits;
		this.clock = clock;
	}

	@Transactional
	CenterVisitResponse checkIn(UUID receptionistId, CenterVisitRequest request) {
		ReceptionMemberSearchResponse member = members.search(receptionistId, request.memberId(), request.phone());
		if (member.status() != AccountStatus.ACTIVE) throw CenterVisitException.memberNotActive();
		if (member.profileImageUrl() == null || member.profileImageUrl().isBlank()) {
			throw CenterVisitException.profileImageRequired();
		}
		Instant now = clock.instant();
		UUID membershipId = visits.findCurrentMembership(member.accountId(), now)
			.orElseThrow(CenterVisitException::membershipRequired);
		CenterVisit visit = visits.save(new CenterVisit(UUID.randomUUID(), member.accountId(), membershipId,
			receptionistId, now));
		return CenterVisitResponse.from(visit, member.memberId());
	}
}
