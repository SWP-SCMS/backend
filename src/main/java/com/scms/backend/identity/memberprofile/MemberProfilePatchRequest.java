package com.scms.backend.identity.memberprofile;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

public final class MemberProfilePatchRequest {

	private boolean fullNamePresent;

	private String fullName;

	private boolean phonePresent;

	private String phone;

	private boolean emailPresent;

	@Schema(hidden = true)
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

	private boolean genderPresent;

	private String gender;

	@JsonSetter("gender")
	public void setGender(String gender) {
		this.genderPresent = true;
		this.gender = gender;
	}

	public boolean hasGender() {
		return genderPresent;
	}

	public String gender() {
		return gender;
	}

	private boolean addressPresent;

	private String address;

	@JsonSetter("address")
	public void setAddress(String address) {
		this.addressPresent = true;
		this.address = address;
	}

	public boolean hasAddress() {
		return addressPresent;
	}

	public String address() {
		return address;
	}

	private boolean physicalConditionNotesPresent;

	private String physicalConditionNotes;

	@JsonSetter("physicalConditionNotes")
	public void setPhysicalConditionNotes(String physicalConditionNotes) {
		this.physicalConditionNotesPresent = true;
		this.physicalConditionNotes = physicalConditionNotes;
	}

	public boolean hasPhysicalConditionNotes() {
		return physicalConditionNotesPresent;
	}

	public String physicalConditionNotes() {
		return physicalConditionNotes;
	}

	private boolean emergencyContactRelationshipPresent;

	private String emergencyContactRelationship;

	@JsonSetter("emergencyContactRelationship")
	public void setEmergencyContactRelationship(String emergencyContactRelationship) {
		this.emergencyContactRelationshipPresent = true;
		this.emergencyContactRelationship = emergencyContactRelationship;
	}

	public boolean hasEmergencyContactRelationship() {
		return emergencyContactRelationshipPresent;
	}

	public String emergencyContactRelationship() {
		return emergencyContactRelationship;
	}

	private boolean emergencyContactTimeNotesPresent;

	private String emergencyContactTimeNotes;

	@JsonSetter("emergencyContactTimeNotes")
	public void setEmergencyContactTimeNotes(String emergencyContactTimeNotes) {
		this.emergencyContactTimeNotesPresent = true;
		this.emergencyContactTimeNotes = emergencyContactTimeNotes;
	}

	public boolean hasEmergencyContactTimeNotes() {
		return emergencyContactTimeNotesPresent;
	}

	public String emergencyContactTimeNotes() {
		return emergencyContactTimeNotes;
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
