package com.scms.backend.auth;

public class DuplicateAccountException extends RuntimeException {

	private final String code;

	private DuplicateAccountException(String code, String message) {
		super(message);
		this.code = code;
	}

	static DuplicateAccountException email() {
		return new DuplicateAccountException("EMAIL_ALREADY_EXISTS",
			"An active or suspended account already uses this email address.");
	}

	static DuplicateAccountException phone() {
		return new DuplicateAccountException("PHONE_ALREADY_EXISTS",
			"An active or suspended account already uses this phone number.");
	}

	static DuplicateAccountException identifier() {
		return new DuplicateAccountException("ACCOUNT_IDENTIFIER_ALREADY_EXISTS",
			"An active or suspended account already uses this email address or phone number.");
	}

	public String getCode() {
		return code;
	}
}
