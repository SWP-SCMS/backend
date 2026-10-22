package com.scms.backend.membership;

public record MembershipTerminationCreateRequest(MembershipTerminationMode mode, String reason) { }
