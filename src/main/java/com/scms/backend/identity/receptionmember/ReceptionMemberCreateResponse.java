package com.scms.backend.identity.receptionmember;

import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import java.time.LocalDate;
import java.util.UUID;

public record ReceptionMemberCreateResponse(UUID accountId, String memberId, String fullName, String phone, String email, LocalDate birthDate, AccountRole role, AccountStatus status) {
}
