package com.scms.backend.manager;

import java.time.LocalDate;
import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.scms.backend.account.AccountRole;

public record StaffAccountCreateRequest(String fullName, String phone, String email, LocalDate birthDate,
		AccountRole role) {

	public StaffAccountCreateRequest {
		if (fullName != null) fullName = fullName.trim();
		if (phone != null) phone = phone.trim();
		if (email != null) email = email.trim().toLowerCase(Locale.ROOT);
	}

	@JsonAnySetter
	public void rejectUnknownField(String fieldName, Object ignoredValue) {
		throw new IllegalArgumentException("Unknown staff account field: " + fieldName);
	}
}
