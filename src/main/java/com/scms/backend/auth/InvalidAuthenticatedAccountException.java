package com.scms.backend.auth;

public class InvalidAuthenticatedAccountException extends RuntimeException {

	public InvalidAuthenticatedAccountException() {
		super("The authenticated account is no longer active");
	}
}
