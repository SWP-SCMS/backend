package com.scms.backend.membership;

import java.math.BigInteger;

public record RefundFirstApprovalRequest(Long expectedVersion, BigInteger approvedAmount) { }
