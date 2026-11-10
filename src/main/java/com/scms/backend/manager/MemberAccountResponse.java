package com.scms.backend.manager;

import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import java.time.LocalDate;
import java.util.UUID;

public record MemberAccountResponse(UUID accountId, String memberId, String fullName, String phone, String email, LocalDate birthDate, AccountRole role, AccountStatus status, String profileImageUrl, String fitnessGoal, String emergencyContactName, String emergencyContactPhone, String activePlanCode) {

	public MemberAccountResponse(UUID accountId, String memberId, String fullName, String phone, String email, LocalDate birthDate, AccountRole role, AccountStatus status, String profileImageUrl, String fitnessGoal, String emergencyContactName, String emergencyContactPhone) {
		this(accountId, memberId, fullName, phone, email, birthDate, role, status, profileImageUrl, fitnessGoal, emergencyContactName, emergencyContactPhone, null);
	}

	MemberAccountResponse withActivePlan(String plan) {
		return new MemberAccountResponse(accountId, memberId, fullName, phone, email, birthDate, role, status, profileImageUrl, fitnessGoal, emergencyContactName, emergencyContactPhone, plan);
	}
}
