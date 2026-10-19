package com.scms.backend.identity.staffprofile;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonAnySetter;

public record StaffProfilePatchRequest(String fullName, String phone, String email, LocalDate birthDate) {
	@JsonAnySetter
	public void rejectUnknownField(String fieldName, Object ignoredValue) {
		throw new IllegalArgumentException("Unknown staff profile field: " + fieldName);
	}
}
