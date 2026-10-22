package com.scms.backend.membership;

import java.time.LocalDate;

public record MembershipFreezeCreateRequest(LocalDate startDate, LocalDate endDate, String reason) { }
