package com.scms.backend.emailverification;

import java.time.Instant;

public record PasswordResetVerifiedResponse(boolean verified, Instant expiresAt) {
}
