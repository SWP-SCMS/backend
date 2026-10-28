package com.scms.backend.common.validation;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import com.scms.backend.auth.RegistrationRequest;
import com.scms.backend.identity.receptionmember.ReceptionMemberCreateRequest;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

class PersonNameRulesTests {
	@Test
	void acceptsVietnameseAndCommonInternationalNameSeparators() {
		assertThat(PersonNameRules.isValid("Nguyễn Thị Ánh")).isTrue();
		assertThat(PersonNameRules.isValid("Jean-Pierre O’Neil")).isTrue();
		assertThat(PersonNameRules.isValid("J. R. R. Tolkien")).isTrue();
	}

	@Test
	void rejectsDigitsSymbolsAndNamesWithoutLetters() {
		assertThat(PersonNameRules.isValid("Member 123")).isFalse();
		assertThat(PersonNameRules.isValid("Name!@#")).isFalse();
		assertThat(PersonNameRules.isValid("---")).isFalse();
		assertThat(PersonNameRules.isValid(" ")).isFalse();
	}

	@Test
	void createMemberRequestsApplyTheSameNameRuleAtTheApiBoundary() {
		try (var factory = Validation.buildDefaultValidatorFactory()) {
			var validator = factory.getValidator();
			var birthDate = LocalDate.of(2000, 1, 1);
			var registration = new RegistrationRequest("Member 123", "0901234567", "member@example.com",
				"Password123", birthDate, null, null);
			var receptionist = new ReceptionMemberCreateRequest("Member 123", "0901234567", "member@example.com",
				birthDate, null);

			assertThat(validator.validate(registration)).extracting(v -> v.getPropertyPath().toString())
				.contains("fullName");
			assertThat(validator.validate(receptionist)).extracting(v -> v.getPropertyPath().toString())
				.contains("fullName");
		}
	}
}
