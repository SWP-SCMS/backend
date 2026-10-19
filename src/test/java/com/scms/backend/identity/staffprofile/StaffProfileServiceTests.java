package com.scms.backend.identity.staffprofile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.UUID;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.auth.AccountIdentifierAvailability;
import com.scms.backend.identity.memberprofile.MemberProfileDetailsValidator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class StaffProfileServiceTests {

	@Mock AccountRepository accounts;
	@Mock AccountIdentifierAvailability identifiers;
	@Mock MemberProfileDetailsValidator validator;

	private StaffProfileService service;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		service = new StaffProfileService(accounts, identifiers, validator);
	}

	@Test
	void updatesOnlyPersonalFieldsForAuthenticatedStaff() {
		UUID id = UUID.randomUUID();
		Account account = new Account(id, AccountRole.COACH, AccountStatus.ACTIVE, "Old", "0900000001",
			"old@example.com", LocalDate.of(1990, 1, 1), "hash");
		when(accounts.findById(id)).thenReturn(java.util.Optional.of(account));
		when(validator.normalizeRequired(" New ", "fullName", 200)).thenReturn("New");
		when(validator.normalizePhone("0900000002", "phone")).thenReturn("0900000002");
		when(validator.normalizeEmail("NEW@example.com")).thenReturn("new@example.com");
		when(validator.validateBirthDate(LocalDate.of(1991, 2, 2))).thenReturn(LocalDate.of(1991, 2, 2));

		StaffProfileResponse response = service.updateProfile(id,
			new StaffProfilePatchRequest(" New ", "0900000002", "NEW@example.com", LocalDate.of(1991, 2, 2)));

		assertThat(response.fullName()).isEqualTo("New");
		assertThat(response.role()).isEqualTo(AccountRole.COACH);
		assertThat(response.status()).isEqualTo(AccountStatus.ACTIVE);
		assertThat(account.getPasswordHash()).isEqualTo("hash");
		verify(identifiers).ensureAvailableForUpdate(id, "old@example.com", "0900000001", "new@example.com",
			"0900000002");
	}
}
