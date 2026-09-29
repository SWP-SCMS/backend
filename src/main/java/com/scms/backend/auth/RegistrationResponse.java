package com.scms.backend.auth;

import java.time.LocalDate;
import java.util.UUID;

import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;

public record RegistrationResponse(UUID accountId, String memberId, String fullName, String phone, String email,
		LocalDate birthDate, AccountRole role, AccountStatus status) {
}
