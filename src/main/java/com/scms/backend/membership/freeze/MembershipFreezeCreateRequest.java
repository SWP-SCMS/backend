package com.scms.backend.membership.freeze;

import java.time.LocalDate;

public record MembershipFreezeCreateRequest(LocalDate startDate, LocalDate endDate, String reason) {
}
