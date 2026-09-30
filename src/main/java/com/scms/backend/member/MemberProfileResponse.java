package com.scms.backend.member;

import java.time.LocalDate;
import java.util.UUID;

import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;

public record MemberProfileResponse(UUID accountId, String memberId, String fullName, String phone, String email,
		LocalDate birthDate, AccountRole role, AccountStatus status, String profileImageUrl, String fitnessGoal,
		String emergencyContactName, String emergencyContactPhone) {
}
