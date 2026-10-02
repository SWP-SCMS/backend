package com.scms.backend.membership;
public record PaymentActionRequest(String status, String providerReference, String evidence, String reason) { }
