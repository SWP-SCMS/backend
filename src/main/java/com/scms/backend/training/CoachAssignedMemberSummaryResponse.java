package com.scms.backend.training;

import java.util.UUID;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.account.MemberProfile;

/**
 * US40: Summary view of a Member in the Coach's assigned list.
 * Includes enough info for the Coach to identify the Member at a glance.
 */
public record CoachAssignedMemberSummaryResponse(
	UUID memberAccountId,
	String memberId,
	String fullName,
	AccountStatus status,
	String profileImageUrl,
	String fitnessGoal,
	UUID assignmentId,
	java.time.Instant assignedSince
) {
	public static CoachAssignedMemberSummaryResponse from(CoachAssignment assignment, Account account,
			MemberProfile profile) {
		return new CoachAssignedMemberSummaryResponse(
			account.getId(),
			profile.getMemberCode(),
			account.getFullName(),
			account.getStatus(),
			profile.getProfileImageUrl(),
			profile.getFitnessGoal(),
			assignment.getId(),
			assignment.getStartsAt()
		);
	}
}
