package com.scms.backend.training;

import java.time.Clock;
import java.util.Optional;
import java.util.UUID;

import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.auth.InvalidAuthenticatedAccountException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MemberCoachAssignmentService {

	private final CoachAssignmentRepository assignments;
	private final AccountRepository accounts;
	private final Clock clock;

	public MemberCoachAssignmentService(CoachAssignmentRepository assignments, AccountRepository accounts,
			Clock clock) {
		this.assignments = assignments;
		this.accounts = accounts;
		this.clock = clock;
	}

	@Transactional
	public Optional<MemberCoachAssignmentResponse> getActive(UUID memberId) {
		if (!accounts.existsByIdAndRoleAndStatus(memberId, AccountRole.MEMBER, AccountStatus.ACTIVE)) {
			throw new InvalidAuthenticatedAccountException();
		}
		assignments.endActiveAssignmentIfPlusExpired(memberId, clock.instant());
		return assignments.findByMemberAccountIdAndStatus(memberId, CoachAssignmentStatus.ACTIVE)
				.flatMap(assignment -> coachResponse(assignment));
	}

	private Optional<MemberCoachAssignmentResponse> coachResponse(CoachAssignment assignment) {
		return accounts.findById(assignment.getCoachAccountId())
				.filter(coach -> coach.getRole() == AccountRole.COACH)
				.map(coach -> MemberCoachAssignmentResponse.from(assignment, coach));
	}
}
