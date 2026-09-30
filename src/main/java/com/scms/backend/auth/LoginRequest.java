package com.scms.backend.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
		@NotBlank @Size(max = 320) String identifier,
		@NotBlank String password) {

	public LoginRequest {
		if (identifier != null) {
			identifier = identifier.trim();
		}
	}

	@Override
	public String toString() {
		return "LoginRequest[identifier=%s, password=[REDACTED]]".formatted(identifier);
	}
}
