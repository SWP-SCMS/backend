package com.scms.backend.auth;

import com.scms.backend.account.AccountRole;
import java.util.UUID;

public record AuthResponse(UUID accountId, String fullName, AccountRole role, String accessToken, String tokenType, long expiresIn) {
}
