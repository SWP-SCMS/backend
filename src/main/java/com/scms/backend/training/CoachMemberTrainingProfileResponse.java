package com.scms.backend.training;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.account.MemberProfile;

/**
 * US41: Full training profile view of a Member for the assigned Personal Coach.
 * Includes profile info and fitness goal data per BR-COA-07.
 */
public record CoachMemberTrainingProfileResponse(
	UUID memberAccountId,
	String memberId,
	String fullName,
	String phone,
	String email,
	LocalDate birthDate,
	AccountStatus status,
	String profileImageUrl,
	String fitnessGoal,
	String emergencyContactName,
	String emergencyContactPhone,
	UUID assignmentId,
	Instant assignedSince
) {
	public static CoachMemberTrainingProfileResponse from(CoachAssignment assignment, Account account,
			MemberProfile profile) {
		return new CoachMemberTrainingProfileResponse(
			account.getId(),
			profile.getMemberCode(),
			account.getFullName(),
			account.getPhone(),
			account.getEmail(),
			account.getBirthDate(),
			account.getStatus(),
			profile.getProfileImageUrl(),
			profile.getFitnessGoal(),
			profile.getEmergencyContactName(),
			profile.getEmergencyContactPhone(),
			assignment.getId(),
			assignment.getStartsAt()
		);
	}
}
