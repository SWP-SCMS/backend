package com.scms.backend.manager;

import com.scms.backend.account.AccountStatus;

public record StaffStatusChangeRequest(AccountStatus status, String reason) {
}
