package com.scms.backend.membership;

public record RefundActionRequest(Long expectedVersion, String reason) { }
