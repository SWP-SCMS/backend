package com.scms.backend.emailverification.verification;

import java.time.Instant;

public record ResendEmailResponse(boolean accepted, Instant resendAvailableAt, Instant serverTime) {
}
