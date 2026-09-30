package com.scms.backend.member;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonSetter;

public final class MemberProfilePatchRequest {

	private boolean fullNamePresent;
	private String fullName;
	private boolean phonePresent;
	private String phone;
	private boolean emailPresent;
	private String email;
	private boolean birthDatePresent;
	private LocalDate birthDate;
	private boolean profileImageUrlPresent;
	private String profileImageUrl;
	private boolean fitnessGoalPresent;
	private String fitnessGoal;
	private boolean emergencyContactNamePresent;
	private String emergencyContactName;
	private boolean emergencyContactPhonePresent;
	private String emergencyContactPhone;

	@JsonSetter("fullName")
	public void setFullName(String fullName) {
		this.fullNamePresent = true;
		this.fullName = fullName;
	}

	@JsonSetter("phone")
	public void setPhone(String phone) {
		this.phonePresent = true;
		this.phone = phone;
	}

	@JsonSetter("email")
	public void setEmail(String email) {
		this.emailPresent = true;
		this.email = email;
	}

	@JsonSetter("birthDate")
	public void setBirthDate(LocalDate birthDate) {
		this.birthDatePresent = true;
		this.birthDate = birthDate;
	}

	@JsonSetter("profileImageUrl")
	public void setProfileImageUrl(String profileImageUrl) {
		this.profileImageUrlPresent = true;
		this.profileImageUrl = profileImageUrl;
	}

	@JsonSetter("fitnessGoal")
	public void setFitnessGoal(String fitnessGoal) {
		this.fitnessGoalPresent = true;
		this.fitnessGoal = fitnessGoal;
	}

	@JsonSetter("emergencyContactName")
	public void setEmergencyContactName(String emergencyContactName) {
		this.emergencyContactNamePresent = true;
		this.emergencyContactName = emergencyContactName;
	}

	@JsonSetter("emergencyContactPhone")
	public void setEmergencyContactPhone(String emergencyContactPhone) {
		this.emergencyContactPhonePresent = true;
		this.emergencyContactPhone = emergencyContactPhone;
	}

	@JsonAnySetter
	public void rejectUnknownField(String fieldName, Object ignoredValue) {
		throw new IllegalArgumentException("Unknown member profile field: " + fieldName);
	}

	public boolean hasFullName() {
		return fullNamePresent;
	}

	public String fullName() {
		return fullName;
	}

	public boolean hasPhone() {
		return phonePresent;
	}

	public String phone() {
		return phone;
	}

	public boolean hasEmail() {
		return emailPresent;
	}

	public String email() {
		return email;
	}

	public boolean hasBirthDate() {
		return birthDatePresent;
	}

	public LocalDate birthDate() {
		return birthDate;
	}

	public boolean hasProfileImageUrl() {
		return profileImageUrlPresent;
	}

	public String profileImageUrl() {
		return profileImageUrl;
	}

	public boolean hasFitnessGoal() {
		return fitnessGoalPresent;
	}

	public String fitnessGoal() {
		return fitnessGoal;
	}

	public boolean hasEmergencyContactName() {
		return emergencyContactNamePresent;
	}

	public String emergencyContactName() {
		return emergencyContactName;
	}

	public boolean hasEmergencyContactPhone() {
		return emergencyContactPhonePresent;
	}

	public String emergencyContactPhone() {
		return emergencyContactPhone;
	}
}
