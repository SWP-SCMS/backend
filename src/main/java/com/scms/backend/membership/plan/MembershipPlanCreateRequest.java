package com.scms.backend.membership.plan;

import com.scms.backend.membership.MembershipPlanStatus;
import java.util.List;

public record MembershipPlanCreateRequest(String code, String displayName, MembershipPlanStatus status, List<String> benefitCodes) {
}
