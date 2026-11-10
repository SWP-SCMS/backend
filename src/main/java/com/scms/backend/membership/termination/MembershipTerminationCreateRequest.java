package com.scms.backend.membership.termination;

import com.scms.backend.membership.MembershipTerminationMode;

public record MembershipTerminationCreateRequest(MembershipTerminationMode mode, String reason) {
}
