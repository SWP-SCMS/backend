package com.scms.backend.identity.memberprofile;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;

public record MemberProfileResponse(UUID accountId, String memberId, String fullName, String phone, String email,
		LocalDate birthDate, AccountRole role, AccountStatus status, String profileImageUrl, String fitnessGoal,
		String emergencyContactName, String emergencyContactPhone,
		String gender, String address, String physicalConditionNotes, String emergencyContactRelationship,
		String emergencyContactTimeNotes, Instant createdAt) {
}
