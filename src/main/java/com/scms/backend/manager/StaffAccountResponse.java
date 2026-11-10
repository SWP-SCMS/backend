package com.scms.backend.manager;

import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import java.time.LocalDate;
import java.util.UUID;

public record StaffAccountResponse(UUID accountId, AccountRole role, AccountStatus status, String fullName, String phone, String email, LocalDate birthDate) {
}
