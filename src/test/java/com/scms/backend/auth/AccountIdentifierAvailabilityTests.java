package com.scms.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.scms.backend.account.AccountRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class AccountIdentifierAvailabilityTests {

	@Mock
	private AccountRepository accountRepository;

	private AccountIdentifierAvailability identifierAvailability;

	@BeforeEach
	void setUp() {
		identifierAvailability = new AccountIdentifierAvailability(accountRepository);
	}

	@Test
	void databaseConstraintNamesMapToStableConflictCodes() {
		DataIntegrityViolationException emailFailure = new DataIntegrityViolationException("outer",
			new IllegalStateException("uq_accounts_current_email"));
		DataIntegrityViolationException phoneFailure = new DataIntegrityViolationException("outer",
			new IllegalStateException("uq_accounts_current_phone"));

		assertThat(identifierAvailability.fromDatabase(emailFailure).getCode()).isEqualTo("EMAIL_ALREADY_EXISTS");
		assertThat(identifierAvailability.fromDatabase(phoneFailure).getCode()).isEqualTo("PHONE_ALREADY_EXISTS");
	}
}
