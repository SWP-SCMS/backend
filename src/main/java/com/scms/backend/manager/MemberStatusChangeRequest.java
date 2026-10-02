package com.scms.backend.manager;

import com.scms.backend.account.AccountStatus;

public record MemberStatusChangeRequest(AccountStatus status, String reason) {
}
