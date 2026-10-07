package com.scms.backend.reception;

import org.springframework.http.HttpStatus;

public class CenterVisitException extends RuntimeException {

	private final HttpStatus status;
	private final String code;

	private CenterVisitException(HttpStatus status, String code, String message) {
		super(message);
		this.status = status;
		this.code = code;
	}

	private CenterVisitException(String code, String message) {
		this(HttpStatus.CONFLICT, code, message);
	}

	static CenterVisitException identityVerificationRequired() {
		return new CenterVisitException(HttpStatus.BAD_REQUEST, "IDENTITY_VERIFICATION_REQUIRED",
			"Identity must be verified before center check-in.");
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

	static CenterVisitException invalidIdempotencyKey() {
		return new CenterVisitException(HttpStatus.BAD_REQUEST, "INVALID_IDEMPOTENCY_KEY",
			"Idempotency-Key must be nonblank and at most 100 characters.");
	}

	static CenterVisitException idempotencyKeyReused() {
		return new CenterVisitException("IDEMPOTENCY_KEY_REUSED",
			"The Idempotency-Key was already used for another Member.");
	}

	static CenterVisitException openVisitNotFound() {
		return new CenterVisitException(HttpStatus.NOT_FOUND, "OPEN_CENTER_VISIT_NOT_FOUND",
			"No current Center Visit was found.");
	}

	static CenterVisitException memberNotFound() {
		return new CenterVisitException(HttpStatus.NOT_FOUND, "MEMBER_NOT_FOUND",
			"The requested Member account was not found.");
	}

	static CenterVisitException invalidPage() {
		return new CenterVisitException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
			"page must be >= 0 and size must be between 1 and 100");
	}

	public HttpStatus getStatus() { return status; }
	public String getCode() { return code; }
}
