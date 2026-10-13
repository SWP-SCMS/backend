package com.scms.backend.training;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.account.MemberProfile;
import com.scms.backend.account.MemberProfileRepository;
import com.scms.backend.auth.InvalidAuthenticatedAccountException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * US40 - Coach - Assigned Member List
 *
 * BR-COA-03: Coach may have multiple ACTIVE assignments.
 * BR-COA-05: Reconcile expired PLUS memberships before listing.
 * BR-COA-06: Only ACTIVE Coach can access.
 * BR-RPT-06: Coach may only see Members within their assigned coaching scope.
 * BR-SEC-04: Role + ownership enforced at backend.
 */
@Service
public class CoachAssignedMemberService {

	private final CoachAssignmentRepository assignments;
	private final AccountRepository accounts;
	private final MemberProfileRepository profiles;
	private final Clock clock;

	public CoachAssignedMemberService(CoachAssignmentRepository assignments, AccountRepository accounts,
			MemberProfileRepository profiles, Clock clock) {
		this.assignments = assignments;
		this.accounts = accounts;
		this.profiles = profiles;
		this.clock = clock;
	}

	/**
	 * US40: List Members currently assigned to this Coach (ACTIVE assignments only).
	 * Reconciles expired PLUS memberships before returning the list.
	 */
	@Transactional
	public CoachAssignedMemberPageResponse listAssignedMembers(UUID coachId, Pageable pageable) {
		ensureActiveCoach(coachId);

		// BR-COA-05: reconcile all expired-PLUS assignments (bulk, no memberId filter)
		assignments.endAllActiveAssignmentsWithExpiredPlus(clock.instant());

		Page<CoachAssignment> page = assignments.findByCoachAccountIdAndStatusOrderByCreatedAtDesc(coachId,
				CoachAssignmentStatus.ACTIVE, pageable);

		List<UUID> memberIds = page.getContent().stream()
				.map(CoachAssignment::getMemberAccountId)
				.toList();

		Map<UUID, Account> accountMap = accounts.findAllById(memberIds).stream()
				.collect(Collectors.toMap(Account::getId, a -> a));

		Map<UUID, MemberProfile> profileMap = profiles.findAllById(memberIds).stream()
				.collect(Collectors.toMap(MemberProfile::getAccountId, p -> p));

		List<CoachAssignedMemberSummaryResponse> content = page.getContent().stream()
				.map(a -> {
					Account account = accountMap.get(a.getMemberAccountId());
					MemberProfile profile = profileMap.get(a.getMemberAccountId());
					return CoachAssignedMemberSummaryResponse.from(a, account, profile);
				})
				.toList();

		return new CoachAssignedMemberPageResponse(content, page.getNumber(), page.getSize(),
				page.getTotalElements(), page.getTotalPages());
	}

	private void ensureActiveCoach(UUID coachId) {
		if (!accounts.existsByIdAndRoleAndStatus(coachId, AccountRole.COACH, AccountStatus.ACTIVE)) {
			throw new InvalidAuthenticatedAccountException();
		}
	}
}
