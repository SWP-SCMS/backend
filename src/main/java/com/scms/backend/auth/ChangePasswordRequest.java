package com.scms.backend.auth;

import com.fasterxml.jackson.annotation.JsonAnySetter;

public record ChangePasswordRequest(
		String currentPassword,
		String newPassword) {

	void validate() {
		if (currentPassword == null || currentPassword.isBlank()) {
			throw new ChangePasswordValidationException("currentPassword", "must not be blank");
		}
		if (newPassword == null || newPassword.isBlank()) {
			throw new ChangePasswordValidationException("newPassword", "must not be blank");
		}
		if (newPassword.length() < 8) {
			throw new ChangePasswordValidationException("newPassword", "size must be at least 8");
		}
	}

	@JsonAnySetter
	public void rejectUnknownField(String fieldName, Object ignoredValue) {
		throw new IllegalArgumentException("Unknown change-password field: " + fieldName);
	}

	@Override
	public String toString() {
		return "ChangePasswordRequest[currentPassword=[REDACTED], newPassword=[REDACTED]]";
	}
}
