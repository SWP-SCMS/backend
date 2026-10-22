package com.scms.backend.emailverification;

import com.fasterxml.jackson.annotation.JsonAnySetter;

import java.nio.charset.StandardCharsets;

public record PasswordResetConfirmRequest(String newPassword, String confirmPassword) {
	void validate() {
		if (newPassword == null || newPassword.isBlank()) invalid("newPassword", "must not be blank");
		if (newPassword.length() < 8) invalid("newPassword", "size must be at least 8");
		if (newPassword.getBytes(StandardCharsets.UTF_8).length > 72)
			invalid("newPassword", "must be at most 72 UTF-8 bytes");
		if (confirmPassword == null || confirmPassword.isBlank()) invalid("confirmPassword", "must not be blank");
		if (!newPassword.equals(confirmPassword)) invalid("confirmPassword", "must match newPassword");
	}

	private void invalid(String field, String message) {
		throw new PasswordResetException(org.springframework.http.HttpStatus.BAD_REQUEST,
			"VALIDATION_ERROR", "One or more request fields are invalid.", field, message);
	}

	@JsonAnySetter
	public void rejectUnknownField(String fieldName, Object ignored) {
		throw new IllegalArgumentException("Unknown password-reset field: " + fieldName);
	}

	@Override public String toString() {
		return "PasswordResetConfirmRequest[newPassword=[REDACTED], confirmPassword=[REDACTED]]";
	}
}
