package com.scms.backend.reception;

import java.time.LocalDate;
import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ReceptionMemberCreateRequest(
		@NotBlank @Size(max = 200) String fullName,
		@NotBlank @Pattern(regexp = "0[0-9]{9}", message = "must be 10 digits starting with 0") String phone,
		@NotBlank @Email @Size(max = 320) String email,
		@NotNull LocalDate birthDate,
		String profileImageUrl) {

	public ReceptionMemberCreateRequest {
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
		throw new IllegalArgumentException("Unknown reception member field: " + fieldName);
	}

	@Override
	public String toString() {
		return ("ReceptionMemberCreateRequest[fullName=%s, phone=[REDACTED], email=%s, birthDate=%s, "
			+ "profileImageUrl=%s]").formatted(fullName, email, birthDate, profileImageUrl);
	}
}
