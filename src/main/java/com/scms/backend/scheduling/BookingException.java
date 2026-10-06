package com.scms.backend.scheduling;

import org.springframework.http.HttpStatus;

public class BookingException extends RuntimeException {

	private final HttpStatus status;
	private final String code;

	private BookingException(HttpStatus status, String code, String message) {
		super(message);
		this.status = status;
		this.code = code;
	}

	static BookingException sessionNotFound() {
		return new BookingException(HttpStatus.NOT_FOUND, "SESSION_NOT_FOUND", "The requested Session was not found.");
	}

	static BookingException memberNotActive() {
		return new BookingException(HttpStatus.CONFLICT, "MEMBER_NOT_ACTIVE", "Booking requires an active Member.");
	}

	static BookingException sessionUnavailable() {
		return new BookingException(HttpStatus.CONFLICT, "SESSION_NOT_BOOKABLE",
			"Booking requires a future scheduled Session.");
	}

	static BookingException plusRequired() {
		return new BookingException(HttpStatus.CONFLICT, "BOOKING_PLUS_MEMBERSHIP_REQUIRED",
			"An active PLUS Membership must cover the Session start time.");
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
}
