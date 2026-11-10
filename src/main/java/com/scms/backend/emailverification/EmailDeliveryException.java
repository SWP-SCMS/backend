package com.scms.backend.emailverification;

import java.time.Duration;

public class EmailDeliveryException extends RuntimeException {

	private final boolean retryable;

	private final String errorName;

	private final Duration retryAfter;

	private EmailDeliveryException(boolean retryable, String errorName, Duration retryAfter, Throwable cause) {
		super(errorName, cause);
		this.retryable = retryable;
		this.errorName = sanitize(errorName);
		this.retryAfter = retryAfter;
	}

	public static EmailDeliveryException retryable(String name) {
		return new EmailDeliveryException(true, name, null, null);
	}

	public static EmailDeliveryException retryable(String name, Duration retryAfter, Throwable cause) {
		return new EmailDeliveryException(true, name, retryAfter, cause);
	}

	public static EmailDeliveryException terminal(String name) {
		return new EmailDeliveryException(false, name, null, null);
	}

	public boolean retryable() {
		return retryable;
	}

	public String errorName() {
		return errorName;
	}

	public Duration retryAfter() {
		return retryAfter;
	}

	private static String sanitize(String value) {
		if (value == null || value.isBlank())
			return "EMAIL_PROVIDER_ERROR";
		String cleaned = value.replaceAll("[^A-Za-z0-9_.-]", "_");
		return cleaned.substring(0, Math.min(cleaned.length(), 120));
	}
}
