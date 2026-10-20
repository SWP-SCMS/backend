package com.scms.backend.emailverification;
import java.time.Instant;
public record ResendEmailResponse(boolean accepted, Instant resendAvailableAt, Instant serverTime) { }
