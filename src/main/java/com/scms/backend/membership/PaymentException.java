package com.scms.backend.membership;

import org.springframework.http.HttpStatus;

public class PaymentException extends RuntimeException {
	private final HttpStatus status;
	private final String code;
	public PaymentException(HttpStatus status, String code, String message) {
		super(message); this.status = status; this.code = code;
	}
	public HttpStatus status() { return status; }
	public String code() { return code; }
	public static PaymentException validation(String message) { return new PaymentException(HttpStatus.BAD_REQUEST, "PAYMENT_VALIDATION_ERROR", message); }
	public static PaymentException unauthorized(String message) { return new PaymentException(HttpStatus.UNAUTHORIZED, "PAYMENT_UNAUTHORIZED", message); }
	public static PaymentException notFound(String message) { return new PaymentException(HttpStatus.NOT_FOUND, "PAYMENT_NOT_FOUND", message); }
	public static PaymentException conflict(String message) { return new PaymentException(HttpStatus.CONFLICT, "PAYMENT_CONFLICT", message); }
	public static PaymentException forbidden() { return new PaymentException(HttpStatus.FORBIDDEN, "PAYMENT_FORBIDDEN", "The authenticated account cannot access this payment resource."); }
}
