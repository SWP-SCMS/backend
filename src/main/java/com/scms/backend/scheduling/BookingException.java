package com.scms.backend.scheduling;

import java.util.List;

import org.springframework.http.HttpStatus;

public class BookingException extends RuntimeException {

	private final HttpStatus status;
	private final String code;
	private final List<String> requiredBenefitCodes;
	private final List<String> missingBenefitCodes;

	private BookingException(HttpStatus status, String code, String message) {
		this(status, code, message, null, null);
	}

	private BookingException(HttpStatus status, String code, String message,
			List<String> requiredBenefitCodes, List<String> missingBenefitCodes) {
		super(message);
		this.status = status;
		this.code = code;
		this.requiredBenefitCodes = requiredBenefitCodes;
		this.missingBenefitCodes = missingBenefitCodes;
	}

	static BookingException sessionNotFound() {
		return new BookingException(HttpStatus.NOT_FOUND, "SESSION_NOT_FOUND", "The requested Session was not found.");
	}

	static BookingException memberNotActive() {
		return new BookingException(HttpStatus.CONFLICT, "MEMBER_NOT_ACTIVE", "Booking requires an active Member.");
	}

	static BookingException actorNotActiveReceptionist() {
		return new BookingException(HttpStatus.FORBIDDEN, "BOOKING_ACTOR_NOT_ACTIVE_RECEPTIONIST",
			"This action requires an active Receptionist account.");
	}

	static BookingException sessionUnavailable() {
		return new BookingException(HttpStatus.CONFLICT, "SESSION_NOT_BOOKABLE",
			"Booking requires a future scheduled Session.");
	}

	static BookingException benefitRequired(List<String> requiredBenefitCodes, List<String> missingBenefitCodes) {
		return new BookingException(HttpStatus.CONFLICT, "MEMBERSHIP_BENEFIT_REQUIRED",
			"An active Membership must provide every required Benefit at the Session start time.",
			List.copyOf(requiredBenefitCodes), List.copyOf(missingBenefitCodes));
	}

	static BookingException duplicate() {
		return new BookingException(HttpStatus.CONFLICT, "BOOKING_DUPLICATE",
			"The Member already has a booked place in this Session.");
	}

	static BookingException timeConflict() {
		return new BookingException(HttpStatus.CONFLICT, "BOOKING_TIME_CONFLICT",
			"The Session overlaps another booked Session for this Member.");
	}

	static BookingException full() {
		return new BookingException(HttpStatus.CONFLICT, "SESSION_FULL", "The Session has no available capacity.");
	}

	static BookingException ptCoachAssignmentMismatch() {
		return new BookingException(HttpStatus.CONFLICT, "PT_COACH_ASSIGNMENT_MISMATCH",
			"A PT_1_1 Session can only be booked with the Member's active Personal Coach.");
	}

	static BookingException concurrentConflict() {
		return new BookingException(HttpStatus.CONFLICT, "BOOKING_CONFLICT",
			"The Booking could not be confirmed because availability changed.");
	}

	static BookingException notFound() {
		return new BookingException(HttpStatus.NOT_FOUND, "BOOKING_NOT_FOUND", "The requested Booking was not found.");
	}

	static BookingException notBooked() {
		return new BookingException(HttpStatus.CONFLICT, "BOOKING_NOT_BOOKED",
			"Only a currently booked Booking can be cancelled.");
	}

	static BookingException cancellationWindowClosed() {
		return new BookingException(HttpStatus.CONFLICT, "BOOKING_CANCELLATION_WINDOW_CLOSED",
			"Booking cancellation must be completed at least two hours before Session start.");
	}

	public HttpStatus getStatus() { return status; }
	public String getCode() { return code; }
	public List<String> getRequiredBenefitCodes() { return requiredBenefitCodes; }
	public List<String> getMissingBenefitCodes() { return missingBenefitCodes; }
}
