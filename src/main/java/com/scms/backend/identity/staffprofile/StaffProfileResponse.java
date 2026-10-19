package com.scms.backend.identity.staffprofile;

import java.time.LocalDate;
import java.util.UUID;

import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;

public record StaffProfileResponse(UUID accountId, String fullName, String phone, String email,
		LocalDate birthDate, AccountRole role, AccountStatus status) { }
