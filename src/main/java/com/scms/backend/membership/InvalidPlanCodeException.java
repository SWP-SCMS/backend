package com.scms.backend.membership;

public class InvalidPlanCodeException extends RuntimeException {

	public InvalidPlanCodeException() {
		super("planCode must be exactly BASIC or PLUS");
	}
}
