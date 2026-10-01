package com.scms.backend.reception;

import java.time.LocalDate;
import java.util.UUID;

import com.scms.backend.account.AccountStatus;

public record ReceptionMemberSearchResponse(UUID accountId, String memberId, String fullName, String phone,
		String email, LocalDate birthDate, String profileImageUrl, AccountStatus status) {
}
