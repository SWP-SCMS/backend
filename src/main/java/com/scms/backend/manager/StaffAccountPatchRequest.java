package com.scms.backend.manager;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonSetter;

public final class StaffAccountPatchRequest {

	private boolean fullNamePresent;
	private String fullName;
	private boolean phonePresent;
	private String phone;
	private boolean emailPresent;
	private String email;
	private boolean birthDatePresent;
	private LocalDate birthDate;

	@JsonSetter("fullName")
	public void setFullName(String value) { fullNamePresent = true; fullName = value; }

	@JsonSetter("phone")
	public void setPhone(String value) { phonePresent = true; phone = value; }

	@JsonSetter("email")
	public void setEmail(String value) { emailPresent = true; email = value; }

	@JsonSetter("birthDate")
	public void setBirthDate(LocalDate value) { birthDatePresent = true; birthDate = value; }

	@JsonAnySetter
	public void rejectUnknownField(String fieldName, Object ignoredValue) {
		throw new IllegalArgumentException("Unknown staff account field: " + fieldName);
	}

	public boolean hasFullName() { return fullNamePresent; }
	public String fullName() { return fullName; }
	public boolean hasPhone() { return phonePresent; }
	public String phone() { return phone; }
	public boolean hasEmail() { return emailPresent; }
	public String email() { return email; }
	public boolean hasBirthDate() { return birthDatePresent; }
	public LocalDate birthDate() { return birthDate; }
}
