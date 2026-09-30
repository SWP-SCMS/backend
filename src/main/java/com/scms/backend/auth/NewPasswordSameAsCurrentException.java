package com.scms.backend.auth;

public class NewPasswordSameAsCurrentException extends RuntimeException {

	public NewPasswordSameAsCurrentException() {
		super("The new password must differ from the current password");
	}
}
