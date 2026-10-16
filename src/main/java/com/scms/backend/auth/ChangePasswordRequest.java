package com.scms.backend.auth;

import com.fasterxml.jackson.annotation.JsonAnySetter;

public record ChangePasswordRequest(
		String currentPassword,
		String newPassword) {

	void validate() {
		if (currentPassword == null || currentPassword.isBlank()) {
			throw new ChangePasswordValidationException("currentPassword", "must not be blank");
		}
		if (!PasswordConstraints.fitsBcrypt(currentPassword)) {
			throw new ChangePasswordValidationException("currentPassword", "must be at most 72 UTF-8 bytes");
		}
		PasswordConstraints.validate("newPassword", newPassword, ChangePasswordValidationException::new);
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
