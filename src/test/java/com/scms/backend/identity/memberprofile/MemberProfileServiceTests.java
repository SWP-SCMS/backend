package com.scms.backend.identity.memberprofile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.account.MemberProfile;
import com.scms.backend.account.MemberProfileRepository;
import com.scms.backend.auth.AccountIdentifierAvailability;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MemberProfileServiceTests {

	@Mock
	private AccountRepository accountRepository;

	@Mock
	private MemberProfileRepository memberProfileRepository;

	@Mock
	private AccountIdentifierAvailability identifierAvailability;

	private MemberProfileService memberProfileService;

	@BeforeEach
	void setUp() {
		Clock clock = Clock.fixed(Instant.parse("2026-09-29T00:00:00Z"), ZoneOffset.UTC);
		memberProfileService = new MemberProfileService(accountRepository, memberProfileRepository,
			identifierAvailability, new MemberProfileDetailsValidator(clock));
	}

	@Test
	void directEmailChangeRequiresVerifiedFlow() {
		UUID accountId = UUID.randomUUID();
		Account account = new Account(accountId, AccountRole.MEMBER, AccountStatus.ACTIVE, "Race Member",
			"0901234567", "old@example.com", LocalDate.of(2000, 1, 15), "bcrypt-hash");
		MemberProfile profile = new MemberProfile(account);
		MemberProfilePatchRequest request = new MemberProfilePatchRequest();
		request.setEmail("new@example.com");
		when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
		when(memberProfileRepository.findById(accountId)).thenReturn(Optional.of(profile));
		assertThatThrownBy(() -> memberProfileService.updateProfile(accountId, request))
			.isInstanceOfSatisfying(com.scms.backend.emailverification.EmailVerificationException.class,
				exception -> assertThat(exception.getCode()).isEqualTo("EMAIL_CHANGE_VERIFICATION_REQUIRED"));
	}

	@Test
	void rejectsDigitsAndSymbolsInMemberFullName() {
		UUID accountId = UUID.randomUUID();
		Account account = new Account(accountId, AccountRole.MEMBER, AccountStatus.ACTIVE, "Valid Member",
			"0901234567", "member@example.com", LocalDate.of(2000, 1, 15), "bcrypt-hash");
		MemberProfile profile = new MemberProfile(account);
		MemberProfilePatchRequest request = new MemberProfilePatchRequest();
		request.setFullName("Member 123!");
		when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
		when(memberProfileRepository.findById(accountId)).thenReturn(Optional.of(profile));

		assertThatThrownBy(() -> memberProfileService.updateProfile(accountId, request))
			.isInstanceOfSatisfying(MemberProfileValidationException.class,
				exception -> assertThat(exception.getField()).isEqualTo("fullName"));
	}
	private MemberProfile existingProfile() {
		UUID id = UUID.randomUUID();
		Account account = new Account(id, AccountRole.MEMBER, AccountStatus.ACTIVE, "Test Member",
			"0901234567", "member@example.com", LocalDate.of(2000, 1, 15), "bcrypt-hash");
		MemberProfile profile = new MemberProfile(account);
		when(accountRepository.findById(id)).thenReturn(Optional.of(account));
		when(memberProfileRepository.findById(id)).thenReturn(Optional.of(profile));
		return profile;
	}

	@Test
	void additionalDetailsAreTrimmedPreservedOnOmissionAndClearedWithNull() {
		MemberProfile profile = existingProfile();
		UUID id = profile.getAccountId();
		MemberProfilePatchRequest patch = new MemberProfilePatchRequest();
		patch.setGender(" FEMALE ");
		patch.setAddress(" 123 Nguyen Trai ");
		patch.setPhysicalConditionNotes(" Knee injury ");
		patch.setEmergencyContactRelationship(" Sister ");
		patch.setEmergencyContactTimeNotes(" After 18:00 ");
		MemberProfileResponse result = memberProfileService.updateProfile(id, patch);
		assertThat(result.gender()).isEqualTo("FEMALE");
		assertThat(result.address()).isEqualTo("123 Nguyen Trai");
		assertThat(result.physicalConditionNotes()).isEqualTo("Knee injury");
		assertThat(result.emergencyContactRelationship()).isEqualTo("Sister");
		assertThat(result.emergencyContactTimeNotes()).isEqualTo("After 18:00");
		assertThat(memberProfileService.updateProfile(id, new MemberProfilePatchRequest())).isEqualTo(result);
		memberProfileService.replaceProfileImage(id, "/profile-images/new.png");
		assertThat(memberProfileService.getProfile(id).physicalConditionNotes()).isEqualTo("Knee injury");
		patch.setGender(null);
		patch.setAddress(null);
		patch.setPhysicalConditionNotes(null);
		patch.setEmergencyContactRelationship(null);
		patch.setEmergencyContactTimeNotes(null);
		MemberProfileResponse cleared = memberProfileService.updateProfile(id, patch);
		assertThat(cleared.gender()).isNull();
		assertThat(cleared.address()).isNull();
		assertThat(cleared.physicalConditionNotes()).isNull();
		assertThat(cleared.emergencyContactRelationship()).isNull();
		assertThat(cleared.emergencyContactTimeNotes()).isNull();
	}

	@org.junit.jupiter.params.ParameterizedTest
	@org.junit.jupiter.params.provider.CsvSource({"gender,16", "address,500", "physicalConditionNotes,2000",
		"emergencyContactRelationship,100", "emergencyContactTimeNotes,500"})
	void invalidAdditionalDetailsDoNotMutateExistingProfile(String field, int limit) throws Exception {
		MemberProfile profile = existingProfile();
		profile.updateAdditionalDetails("MALE", "Original address", "Original notes", "Parent", "Evenings");
		MemberProfileResponse before = memberProfileService.getProfile(profile.getAccountId());
		var setter = MemberProfilePatchRequest.class.getMethod("set" + Character.toUpperCase(field.charAt(0))
			+ field.substring(1), String.class);
		for (String value : new String[] {"   ", "x".repeat(limit + 1)}) {
			MemberProfilePatchRequest patch = new MemberProfilePatchRequest();
			patch.setFullName("Changed Name");
			setter.invoke(patch, value);
			assertThatThrownBy(() -> memberProfileService.updateProfile(profile.getAccountId(), patch))
				.isInstanceOfSatisfying(MemberProfileValidationException.class,
					error -> assertThat(error.getField()).isEqualTo(field));
			assertThat(memberProfileService.getProfile(profile.getAccountId())).isEqualTo(before);
		}
	}

	@Test
	void genderRejectsUnknownValues() {
		MemberProfile profile = existingProfile();
		MemberProfilePatchRequest patch = new MemberProfilePatchRequest();
		patch.setGender("OTHER");
		assertThatThrownBy(() -> memberProfileService.updateProfile(profile.getAccountId(), patch))
			.isInstanceOfSatisfying(MemberProfileValidationException.class,
				error -> assertThat(error.getField()).isEqualTo("gender"));
	}

	@Test
	void jsonPatchDistinguishesOmissionFromNullForEveryAdditionalField() throws Exception {
		var mapper = tools.jackson.databind.json.JsonMapper.builder().build();
		String[] fields = {"gender", "address", "physicalConditionNotes", "emergencyContactRelationship",
			"emergencyContactTimeNotes"};
		for (String field : fields) {
			String capitalized = Character.toUpperCase(field.charAt(0)) + field.substring(1);
			var has = MemberProfilePatchRequest.class.getMethod("has" + capitalized);
			var value = MemberProfilePatchRequest.class.getMethod(field);
			var omitted = mapper.readValue("{}", MemberProfilePatchRequest.class);
			var cleared = mapper.readValue("{\"%s\":null}".formatted(field), MemberProfilePatchRequest.class);
			var provided = mapper.readValue("{\"%s\":\"value\"}".formatted(field), MemberProfilePatchRequest.class);
			assertThat(has.invoke(omitted)).isEqualTo(false);
			assertThat(has.invoke(cleared)).isEqualTo(true);
			assertThat(value.invoke(cleared)).isNull();
			assertThat(has.invoke(provided)).isEqualTo(true);
			assertThat(value.invoke(provided)).isEqualTo("value");
		}
	}

}
