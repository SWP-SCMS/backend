package com.scms.backend.auth;

import java.time.LocalDate;
import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegistrationRequest(
		@NotBlank @Size(max = 200) String fullName,
		@NotBlank @Pattern(regexp = "0[0-9]{9}", message = "must be 10 digits starting with 0") String phone,
		@NotBlank @Email @Size(max = 320) String email,
		String password,
		@NotNull LocalDate birthDate,
		String profileImageUrl,
		String fitnessGoal) {

	public RegistrationRequest {
		if (fullName != null) {
			fullName = fullName.trim();
		}
		if (phone != null) {
			phone = phone.trim();
		}
		if (email != null) {
			email = email.trim().toLowerCase(Locale.ROOT);
		}
	}

	@JsonAnySetter
	public void rejectUnknownField(String fieldName, Object ignoredValue) {
		throw new IllegalArgumentException("Unknown registration field: " + fieldName);
	}

	@Override
	public String toString() {
		return ("RegistrationRequest[fullName=%s, phone=%s, email=%s, password=[REDACTED], birthDate=%s, "
			+ "profileImageUrl=%s, fitnessGoal=%s]").formatted(fullName, phone, email, birthDate, profileImageUrl,
				fitnessGoal);
	}
}
