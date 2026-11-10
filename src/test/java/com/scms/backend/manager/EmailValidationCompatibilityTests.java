package com.scms.backend.manager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.scms.backend.identity.memberprofile.MemberProfileDetailsValidator;
import com.scms.backend.identity.memberprofile.MemberProfileValidationException;
import java.time.Clock;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class EmailValidationCompatibilityTests {

	// Bounded synthetic inputs characterize the old acceptance rules, including unusual dot placement.
	private static final Pattern LEGACY_EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

	private final MemberProfileDetailsValidator member = new MemberProfileDetailsValidator(Clock.systemUTC());

	private final StaffAccountService staff = new StaffAccountService(null, null, Clock.systemUTC());

	private final List<Function<String, String>> validators = List.of(member::normalizeEmail,
		value -> ReflectionTestUtils.invokeMethod(staff, "normalizeEmail", value));

	@Test
	void bothValidatorsPreserveLegacyAcceptanceForEmailSeparatorsAndWhitespace() {
		char[] alphabet = {'a', '.', '@', ' ', '\t', '\n'};
		for (int length = 0; length <= 5; length++) {
			int combinations = (int) Math.pow(alphabet.length, length);
			for (int value = 0; value < combinations; value++) {
				int remaining = value;
				StringBuilder candidate = new StringBuilder();
				for (int digit = 0; digit < length; digit++) {
					candidate.append(alphabet[remaining % alphabet.length]);
					remaining /= alphabet.length;
				}
				assertAcceptance("u@" + candidate);
				assertAcceptance(candidate + "@a.b");
			}
		}
		for (String value : List.of("!@!." + "!.".repeat(150), "a@.b", "a@..b", "a@b..",
			"a@b.c\u0085", "a@b.c\u00a0", "\u0130@EXAMPLE.COM", "a@b.c\r\n")) {
			assertAcceptance(value);
		}
	}

	@Test
	void keepsNormalizationAndTheExistingLengthBoundary() {
		String boundary = "u@" + "a".repeat(314) + ".com";
		assertThat(boundary).hasSize(320);
		for (Function<String, String> validator : validators) {
			assertThat(validator.apply(" \tUSER@Example.COM\r\n")).isEqualTo("user@example.com");
			assertThat(validator.apply(boundary)).isEqualTo(boundary);
			assertThatThrownBy(() -> validator.apply(boundary + "x")).hasMessageContaining("320");
		}
	}

	@Test
	void malformedEmailKeepsTheExistingFieldAndErrorMessage() {
		assertThatThrownBy(() -> member.normalizeEmail("bad@domain"))
			.isInstanceOf(MemberProfileValidationException.class)
			.hasMessage("must be a well-formed email address")
			.hasFieldOrPropertyWithValue("field", "email");
		assertThatThrownBy(() -> validators.get(1).apply("bad@domain"))
			.isInstanceOf(StaffAccountValidationException.class)
			.hasMessage("must be a well-formed email address")
			.hasFieldOrPropertyWithValue("field", "email");
	}

	private void assertAcceptance(String value) {
		String normalized = value.trim().toLowerCase(Locale.ROOT);
		boolean accepted = LEGACY_EMAIL.matcher(normalized).matches();
		for (Function<String, String> validator : validators) {
			if (accepted) {
				assertThat(validator.apply(value)).isEqualTo(normalized);
			} else {
				assertThatThrownBy(() -> validator.apply(value))
					.isInstanceOfAny(MemberProfileValidationException.class, StaffAccountValidationException.class);
			}
		}
	}
}
