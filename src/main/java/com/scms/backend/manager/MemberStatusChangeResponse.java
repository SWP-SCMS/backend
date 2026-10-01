package com.scms.backend.manager;

import java.util.UUID;

import com.scms.backend.account.AccountStatus;

public record MemberStatusChangeResponse(UUID accountId, AccountStatus status, int cancelledFutureBookings) {
}
