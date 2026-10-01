package com.scms.backend.reception;

public class ReceptionMemberNotFoundException extends RuntimeException {

	public ReceptionMemberNotFoundException() {
		super("The requested member was not found.");
	}
}
