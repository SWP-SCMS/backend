package com.scms.backend.identity.staffprofile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.UUID;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.auth.AccountIdentifierAvailability;
import com.scms.backend.auth.DuplicateAccountException;
import com.scms.backend.audit.AuditEvent;
import com.scms.backend.audit.AuditEventRepository;
import com.scms.backend.identity.memberprofile.MemberProfileDetailsValidator;
import com.scms.backend.emailverification.AccountEmailChangeService;
import com.scms.backend.emailverification.AccountEmailChangeStatus;
import com.scms.backend.emailverification.EmailVerificationException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.DataIntegrityViolationException;

class StaffProfileServiceTests {

	@Mock AccountRepository accounts;
	@Mock AccountIdentifierAvailability identifiers;
	@Mock MemberProfileDetailsValidator validator;
	@Mock AuditEventRepository audits;
	@Mock AccountEmailChangeService emailChanges;

	private StaffProfileService service;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		service = new StaffProfileService(accounts, identifiers, validator, audits, emailChanges);
		when(emailChanges.status(any())).thenReturn(AccountEmailChangeStatus.none());
	}

	@Test
	void updatesOnlyPersonalFieldsForAuthenticatedStaff() {
		UUID id = UUID.randomUUID();
		Account account = new Account(id, AccountRole.COACH, AccountStatus.ACTIVE, "Old", "0900000001",
			"old@example.com", LocalDate.of(1990, 1, 1), "hash");
		when(accounts.findById(id)).thenReturn(java.util.Optional.of(account));
		when(validator.normalizePersonName(" New ", "fullName", 200)).thenReturn("New");
		when(validator.normalizePhone("0900000002", "phone")).thenReturn("0900000002");
		when(validator.validateBirthDate(LocalDate.of(1991, 2, 2))).thenReturn(LocalDate.of(1991, 2, 2));

		StaffProfileResponse response = service.updateProfile(id,
			new StaffProfilePatchRequest(" New ", "0900000002", null, LocalDate.of(1991, 2, 2)));

		assertThat(response.fullName()).isEqualTo("New");
		assertThat(response.role()).isEqualTo(AccountRole.COACH);
		assertThat(response.status()).isEqualTo(AccountStatus.ACTIVE);
		assertThat(account.getPasswordHash()).isEqualTo("hash");
		verify(identifiers).ensureAvailableForUpdate(id, "old@example.com", "0900000001", "old@example.com",
			"0900000002");
		var audit = org.mockito.ArgumentCaptor.forClass(AuditEvent.class);
		verify(audits).save(audit.capture());
		assertThat(audit.getValue().getAction()).isEqualTo("STAFF_SELF_PROFILE_UPDATED");
		assertThat(audit.getValue().getTargetType()).isEqualTo("ACCOUNT");
		assertThat(audit.getValue().getActorAccountId()).isEqualTo(id);
		assertThat(audit.getValue().getTargetId()).isEqualTo(id);
		assertThat(audit.getValue().getBeforeData()).containsEntry("fullName", "Old")
			.containsEntry("email", "o***@example.com").containsEntry("phone", "******0001")
			.doesNotContainKeys("password", "passwordHash", "role", "status");
		assertThat(audit.getValue().getAfterData()).containsEntry("fullName", "New")
			.containsEntry("email", "o***@example.com").containsEntry("phone", "******0002")
			.doesNotContainKeys("password", "passwordHash", "role", "status");
		var persistenceOrder = inOrder(accounts, audits);
		persistenceOrder.verify(accounts).flush();
		persistenceOrder.verify(audits).save(any());
	}

	@Test
	void directEmailChangeRequiresVerifiedFlow() {
		UUID id = UUID.randomUUID(); Account account = account(id);
		when(accounts.findById(id)).thenReturn(java.util.Optional.of(account));
		when(validator.normalizeEmail("new@example.com")).thenReturn("new@example.com");

		assertThatThrownBy(() -> service.updateProfile(id,
			new StaffProfilePatchRequest(null, null, "new@example.com", null)))
			.isInstanceOfSatisfying(EmailVerificationException.class,
				exception -> assertThat(exception.getCode()).isEqualTo("EMAIL_CHANGE_VERIFICATION_REQUIRED"));
		assertThat(account.getEmail()).isEqualTo("old@example.com");
		verify(accounts, never()).flush();
	}

	@Test
	void identicalPatchDoesNotWriteAuditOrFlush() {
		UUID id = UUID.randomUUID();
		Account account = account(id);
		when(accounts.findById(id)).thenReturn(java.util.Optional.of(account));

		StaffProfileResponse response = service.updateProfile(id, new StaffProfilePatchRequest(null, null, null, null));

		assertThat(response.fullName()).isEqualTo("Old");
		verify(accounts, never()).flush();
		verify(audits, never()).save(any());
	}

	@Test
	void replacesAndReturnsStaffProfileImage() {
		UUID id = UUID.randomUUID();
		Account account = account(id);
		when(accounts.findById(id)).thenReturn(java.util.Optional.of(account));

		assertThat(service.replaceProfileImage(id, "/profile-images/new.png")).isNull();
		assertThat(service.getProfile(id).profileImageUrl()).isEqualTo("/profile-images/new.png");
		verify(accounts).flush();
	}

	@Test
	void databaseUniqueRaceIsTranslatedBeforeAuditIsWritten() {
		UUID id = UUID.randomUUID();
		Account account = account(id);
		DataIntegrityViolationException databaseFailure = new DataIntegrityViolationException(
			"duplicate key value violates unique constraint uq_accounts_current_email");
		DuplicateAccountException translated = org.mockito.Mockito.mock(DuplicateAccountException.class);
		when(accounts.findById(id)).thenReturn(java.util.Optional.of(account));
		when(validator.normalizePhone("0900000099", "phone")).thenReturn("0900000099");
		doThrow(databaseFailure).when(accounts).flush();
		when(identifiers.fromDatabase(databaseFailure)).thenReturn(translated);

		assertThatThrownBy(() -> service.updateProfile(id,
			new StaffProfilePatchRequest(null, "0900000099", null, null))).isSameAs(translated);

		verify(identifiers).fromDatabase(databaseFailure);
		verify(audits, never()).save(any());
	}

	@Test
	void contactCanBeSavedPreservedAndClearedWithRealValidation() {
        service = new StaffProfileService(accounts, identifiers,
            new MemberProfileDetailsValidator(java.time.Clock.systemUTC()), audits, emailChanges);
        UUID id = UUID.randomUUID(); Account account = account(id);
        when(accounts.findById(id)).thenReturn(java.util.Optional.of(account));
        var saved = service.updateProfile(id, new StaffProfilePatchRequest(null, null, null, null,
            " Nguyen Van B ", "0901234567", " Anh trai "));
        assertThat(saved.emergencyContactName()).isEqualTo("Nguyen Van B");
        assertThat(saved.emergencyContactRelationship()).isEqualTo("Anh trai");
        assertThat(service.updateProfile(id, new StaffProfilePatchRequest("New Name", null, null, null))
            .emergencyContactPhone()).isEqualTo("0901234567");
        var cleared = service.updateProfile(id, new StaffProfilePatchRequest(null, null, null, null, "", "", ""));
        assertThat(cleared.emergencyContactName()).isNull();
        assertThat(cleared.emergencyContactPhone()).isNull();
        assertThat(cleared.emergencyContactRelationship()).isNull();
    }

    @Test
    void invalidOrIncompleteContactDoesNotMutateAccount() {
        service = new StaffProfileService(accounts, identifiers,
            new MemberProfileDetailsValidator(java.time.Clock.systemUTC()), audits, emailChanges);
        UUID id = UUID.randomUUID(); Account account = account(id);
        when(accounts.findById(id)).thenReturn(java.util.Optional.of(account));
        for (String phone : new String[] { "", "bad-number" }) {
            assertThatThrownBy(() -> service.updateProfile(id, new StaffProfilePatchRequest("New Name", null,
                null, null, "Nguyen Van B", phone, "Anh")))
                .isInstanceOf(com.scms.backend.identity.memberprofile.MemberProfileValidationException.class);
        }
        assertThat(account.getFullName()).isEqualTo("Old");
        assertThat(account.getStaffEmergencyContactName()).isNull();
        verify(accounts, never()).flush();
    }

    @Test
    void staffImageCanBeRemoved() {
        UUID id = UUID.randomUUID(); Account account = account(id);
        account.replaceStaffProfileImage("/profile-images/old.png");
        when(accounts.findById(id)).thenReturn(java.util.Optional.of(account));
        assertThat(service.replaceProfileImage(id, null)).isEqualTo("/profile-images/old.png");
        assertThat(service.getProfile(id).profileImageUrl()).isNull();
    }

	private Account account(UUID id) {
		return new Account(id, AccountRole.COACH, AccountStatus.ACTIVE, "Old", "0900000001",
			"old@example.com", LocalDate.of(1990, 1, 1), "hash");
	}
}
