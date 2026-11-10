package com.scms.backend.membership.refund;

import java.math.BigInteger;

public record RefundFirstApprovalRequest(Long expectedVersion, BigInteger approvedAmount) {
}
