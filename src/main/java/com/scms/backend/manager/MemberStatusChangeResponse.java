package com.scms.backend.manager;

import com.scms.backend.account.AccountStatus;
import java.util.UUID;

public record MemberStatusChangeResponse(UUID accountId, AccountStatus status, int cancelledFutureBookings) {
}
