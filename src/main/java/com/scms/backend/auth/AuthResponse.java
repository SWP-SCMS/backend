package com.scms.backend.auth;

import java.util.UUID;

import com.scms.backend.account.AccountRole;

public record AuthResponse(UUID accountId, String fullName, AccountRole role, String accessToken, String tokenType,
		long expiresIn) {
}
