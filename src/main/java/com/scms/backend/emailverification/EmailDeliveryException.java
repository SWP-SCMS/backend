package com.scms.backend.emailverification;

import java.time.Duration;

class EmailDeliveryException extends RuntimeException {
	private final boolean retryable;
	private final String errorName;
	private final Duration retryAfter;

	private EmailDeliveryException(boolean retryable, String errorName, Duration retryAfter, Throwable cause) {
		super(errorName, cause);
		this.retryable = retryable;
		this.errorName = sanitize(errorName);
		this.retryAfter = retryAfter;
	}

	static EmailDeliveryException retryable(String name) {
		return new EmailDeliveryException(true, name, null, null);
	}

	static EmailDeliveryException retryable(String name, Duration retryAfter, Throwable cause) {
		return new EmailDeliveryException(true, name, retryAfter, cause);
	}

	static EmailDeliveryException terminal(String name) {
		return new EmailDeliveryException(false, name, null, null);
	}

	boolean retryable() { return retryable; }
	String errorName() { return errorName; }
	Duration retryAfter() { return retryAfter; }

	private static String sanitize(String value) {
		if (value == null || value.isBlank()) return "EMAIL_PROVIDER_ERROR";
		String cleaned = value.replaceAll("[^A-Za-z0-9_.-]", "_");
		return cleaned.substring(0, Math.min(cleaned.length(), 120));
	}
}
