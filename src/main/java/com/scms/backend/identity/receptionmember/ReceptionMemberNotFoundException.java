package com.scms.backend.identity.receptionmember;

public class ReceptionMemberNotFoundException extends RuntimeException {

	public ReceptionMemberNotFoundException() {
		super("The requested member was not found.");
	}
}
