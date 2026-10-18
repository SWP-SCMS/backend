package com.scms.backend.membership;

public class InvalidPlanCodeException extends RuntimeException {

	public InvalidPlanCodeException() {
		super("planCode must be uppercase and contain only letters, digits, or underscores");
	}
}
