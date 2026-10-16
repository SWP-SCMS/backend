package com.scms.backend.manager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.account.MemberProfile;
import com.scms.backend.audit.AuditEventRepository;
import com.scms.backend.auth.RefreshTokenService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class ResetPasswordServiceTests {
	@Mock AccountRepository accounts;
	@Mock AuditEventRepository audits;
	@Mock RefreshTokenService refreshTokens;
	@Mock PasswordEncoder encoder;
	@Mock com.scms.backend.account.MemberProfileRepository profiles;

	@Test
	void managerResetHashesCurrentPhoneAndRevokesRefreshTokens() {
		UUID actor = UUID.randomUUID();
		UUID targetId = UUID.randomUUID();
		Account target = new Account(targetId, AccountRole.COACH, AccountStatus.ACTIVE, "Coach", "0901234567",
			"coach@example.com", LocalDate.of(1990, 1, 1), "old");
		when(accounts.existsByIdAndRoleAndStatus(actor, AccountRole.MANAGER, AccountStatus.ACTIVE)).thenReturn(true);
		when(accounts.findByIdForUpdate(targetId)).thenReturn(Optional.of(target));
		when(encoder.encode("0901234567")).thenReturn("new-hash");

		new ResetPasswordService(accounts, audits, refreshTokens, encoder, profiles).managerReset(actor, targetId);

		assertThat(target.getPasswordHash()).isEqualTo("new-hash");
		verify(refreshTokens).revokeAll(targetId);
		verify(audits).save(any());
	}

	@Test
	void receptionistResetLocksMemberBeforeChangingPassword() {
		UUID actor = UUID.randomUUID();
		Account target = account(AccountRole.MEMBER, AccountStatus.ACTIVE);
		when(accounts.existsByIdAndRoleAndStatus(actor, AccountRole.RECEPTIONIST, AccountStatus.ACTIVE))
			.thenReturn(true);
		when(profiles.findVisibleMemberByCode("MEM-001", AccountRole.MEMBER,
			java.util.List.of(AccountStatus.ACTIVE))).thenReturn(Optional.of(new MemberProfile(target)));
		when(accounts.findByIdForUpdate(target.getId())).thenReturn(Optional.of(target));
		when(encoder.encode(target.getPhone())).thenReturn("new-hash");

		new ResetPasswordService(accounts, audits, refreshTokens, encoder, profiles)
			.receptionistResetMember(actor, "MEM-001");

		assertThat(target.getPasswordHash()).isEqualTo("new-hash");
		verify(accounts).findByIdForUpdate(target.getId());
		verify(refreshTokens).revokeAll(target.getId());
	}

	@Test
	void receptionistResetRejectsLockedNonMember() {
		assertReceptionistRejectsLocked(account(AccountRole.COACH, AccountStatus.ACTIVE));
	}

	@Test
	void receptionistResetRejectsLockedInactiveMember() {
		assertReceptionistRejectsLocked(account(AccountRole.MEMBER, AccountStatus.SUSPENDED));
	}

	private void assertReceptionistRejectsLocked(Account lockedTarget) {
		UUID actor = UUID.randomUUID();
		Account visibleTarget = new Account(lockedTarget.getId(), AccountRole.MEMBER, AccountStatus.ACTIVE,
			"Member", lockedTarget.getPhone(), lockedTarget.getEmail(), LocalDate.of(1990, 1, 1), "old");
		when(accounts.existsByIdAndRoleAndStatus(actor, AccountRole.RECEPTIONIST, AccountStatus.ACTIVE))
			.thenReturn(true);
		when(profiles.findVisibleMemberByCode(eq("MEM-001"), eq(AccountRole.MEMBER), any()))
			.thenReturn(Optional.of(new MemberProfile(visibleTarget)));
		when(accounts.findByIdForUpdate(lockedTarget.getId())).thenReturn(Optional.of(lockedTarget));

		assertThatThrownBy(() -> new ResetPasswordService(accounts, audits, refreshTokens, encoder, profiles)
			.receptionistResetMember(actor, "MEM-001"))
			.isInstanceOf(MemberAccountNotFoundException.class);
		verify(encoder, never()).encode(any());
		verify(refreshTokens, never()).revokeAll(any());
	}

	private Account account(AccountRole role, AccountStatus status) {
		UUID id = UUID.randomUUID();
		return new Account(id, role, status, "Member", "0901234567", id + "@example.test",
			LocalDate.of(1990, 1, 1), "old");
	}
}
