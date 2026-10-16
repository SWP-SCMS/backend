package com.scms.backend.auth;

import java.nio.charset.StandardCharsets;

public final class PasswordConstraints {

	static final int MIN_LENGTH = 8;
	static final int MAX_BCRYPT_BYTES = 72;

	private PasswordConstraints() {
	}

	static void validate(String field, String password, RuntimeExceptionFactory errors) {
		if (password == null || password.isBlank()) {
			throw errors.create(field, "must not be blank");
		}
		if (password.length() < MIN_LENGTH) {
			throw errors.create(field, "size must be at least 8");
		}
		if (password.getBytes(StandardCharsets.UTF_8).length > MAX_BCRYPT_BYTES) {
			throw errors.create(field, "must be at most 72 UTF-8 bytes");
		}
	}

	public static boolean fitsBcrypt(String password) {
		return password != null && password.getBytes(StandardCharsets.UTF_8).length <= MAX_BCRYPT_BYTES;
	}

	@FunctionalInterface
	interface RuntimeExceptionFactory {
		RuntimeException create(String field, String message);
	}
}
