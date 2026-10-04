package com.scms.backend.manager;

import java.time.LocalDate;
import java.util.UUID;

import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;

public record MemberAccountResponse(UUID accountId, String memberId, String fullName, String phone, String email,
		LocalDate birthDate, AccountRole role, AccountStatus status, String profileImageUrl, String fitnessGoal,
		String emergencyContactName, String emergencyContactPhone) {
}
