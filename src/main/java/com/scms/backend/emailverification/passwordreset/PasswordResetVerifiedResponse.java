package com.scms.backend.emailverification.passwordreset;

import java.time.Instant;

public record PasswordResetVerifiedResponse(boolean verified, Instant expiresAt) {
}
