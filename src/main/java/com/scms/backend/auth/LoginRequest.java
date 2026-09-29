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
}
