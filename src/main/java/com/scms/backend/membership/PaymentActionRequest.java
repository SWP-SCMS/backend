package com.scms.backend.membership;
public record PaymentActionRequest(String providerReference, String evidence, String reason) { }
