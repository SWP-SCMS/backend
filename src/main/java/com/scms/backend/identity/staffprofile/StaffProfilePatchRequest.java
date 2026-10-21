package com.scms.backend.identity.staffprofile;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import io.swagger.v3.oas.annotations.media.Schema;

public record StaffProfilePatchRequest(String fullName, String phone, @Schema(hidden=true) String email, LocalDate birthDate) {
	@JsonAnySetter
	public void rejectUnknownField(String fieldName, Object ignoredValue) {
		throw new IllegalArgumentException("Unknown staff profile field: " + fieldName);
	}
}
