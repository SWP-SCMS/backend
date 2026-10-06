package com.scms.backend.reception;

import org.springframework.http.HttpStatus;

public class CenterVisitException extends RuntimeException {

	private final HttpStatus status;
	private final String code;

	private CenterVisitException(String code, String message) {
		super(message);
		this.status = HttpStatus.CONFLICT;
		this.code = code;
	}

	static CenterVisitException memberNotActive() {
		return new CenterVisitException("CENTER_VISIT_MEMBER_NOT_ACTIVE",
			"Center check-in requires an active Member account.");
	}

	static CenterVisitException profileImageRequired() {
		return new CenterVisitException("CENTER_VISIT_PROFILE_IMAGE_REQUIRED",
			"A profile image is required before center check-in.");
	}

	static CenterVisitException membershipRequired() {
		return new CenterVisitException("CENTER_VISIT_MEMBERSHIP_REQUIRED",
			"An active BASIC or PLUS Membership is required for center check-in.");
	}

	public HttpStatus getStatus() { return status; }
	public String getCode() { return code; }
}
