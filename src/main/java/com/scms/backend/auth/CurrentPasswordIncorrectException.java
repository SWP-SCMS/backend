package com.scms.backend.auth;

public class CurrentPasswordIncorrectException extends RuntimeException {

	public CurrentPasswordIncorrectException() {
		super("The current password is incorrect");
	}
}
