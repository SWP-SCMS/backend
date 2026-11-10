package com.scms.backend.membership.refund;

public record RefundActionRequest(Long expectedVersion, String reason) {
}
