package com.scms.backend.member;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Locale;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

@Component
public class MemberProfileDetailsValidator {

	private static final Pattern PHONE_PATTERN = Pattern.compile("0[0-9]{9}");
	private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

	private final Clock clock;

	public MemberProfileDetailsValidator(Clock clock) {
		this.clock = clock;
	}

	public String normalizeRequired(String value, String field, int maxLength) {
		if (value == null) {
			throw invalid(field, "must not be null");
		}
		String normalized = value.trim();
		if (normalized.isEmpty()) {
			throw invalid(field, "must not be blank");
		}
		if (normalized.length() > maxLength) {
			throw invalid(field, "must not exceed " + maxLength + " characters");
		}
		return normalized;
	}

	public String normalizePhone(String value, String field) {
		String normalized = normalizeRequired(value, field, 32);
		if (!PHONE_PATTERN.matcher(normalized).matches()) {
			throw invalid(field, "must be 10 digits starting with 0");
		}
		return normalized;
	}

	public String normalizeEmail(String value) {
		String normalized = normalizeRequired(value, "email", 320).toLowerCase(Locale.ROOT);
		if (!EMAIL_PATTERN.matcher(normalized).matches()) {
			throw invalid("email", "must be a well-formed email address");
		}
		return normalized;
	}

	public LocalDate validateBirthDate(LocalDate value) {
		if (value == null) {
			throw invalid("birthDate", "must not be null");
		}
		if (value.isAfter(LocalDate.now(clock))) {
			throw invalid("birthDate", "must not be in the future");
		}
		return value;
	}

	public String normalizeOptional(String value, String field, Integer maxLength) {
		if (value == null) {
			return null;
		}
		String normalized = value.trim();
		if (normalized.isEmpty()) {
			throw invalid(field, "must be null or non-blank");
		}
		if (maxLength != null && normalized.length() > maxLength) {
			throw invalid(field, "must not exceed " + maxLength + " characters");
		}
		return normalized;
	}

	public String normalizeOptionalPhone(String value) {
		if (value == null) {
			return null;
		}
		return normalizePhone(value, "emergencyContactPhone");
	}

	public void validateEmergencyContact(String name, String phone) {
		if ((name == null) != (phone == null)) {
			throw invalid("emergencyContact", "name and phone must both be provided or both be null");
		}
	}

	private MemberProfileValidationException invalid(String field, String message) {
		return new MemberProfileValidationException(field, message);
	}
}
