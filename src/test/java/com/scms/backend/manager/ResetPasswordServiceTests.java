package com.scms.backend.manager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
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
		when(accounts.findById(targetId)).thenReturn(Optional.of(target));
		when(encoder.encode("0901234567")).thenReturn("new-hash");

		new ResetPasswordService(accounts, audits, refreshTokens, encoder, profiles).managerReset(actor, targetId);

		assertThat(target.getPasswordHash()).isEqualTo("new-hash");
		verify(refreshTokens).revokeAll(targetId);
		verify(audits).save(any());
	}
}
